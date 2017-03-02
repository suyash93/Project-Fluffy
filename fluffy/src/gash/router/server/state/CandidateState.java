package gash.router.server.state;

import gash.router.inmemory.RiakClientHandler;
import gash.router.message.command.LeaderStatus;
import gash.router.message.work.*;
import gash.router.persistence.CouchDBHandler;
import gash.router.server.AdministerQueue;
import gash.router.server.MessageServer;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.utilities.ElectionTimeoutListener;
import gash.router.server.utilities.Timer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import pipe.common.Common;
import pipe.leaderelection.Leaderelection;
import pipe.work.Work;
import pipe.work.Work.WorkMessage;
import routing.Pipe;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Candidate State
 *
 
 */
public class CandidateState extends ServerState implements ElectionTimeoutListener {
    private static Logger logger = LoggerFactory.getLogger("Candidate State");
    private int nodeID;
    private Timer timer;
    private HashMap<Integer, Integer> voteMap;
    private int requiredVotes = 1;
    private int sizeOfCluster = 1;
    private boolean fileExists = false;

    public CandidateState(MessageServer server) {
        super(server);
        nodeID = server.getConf().getNodeId();
        timer = new Timer(server.getConf().getHeartbeatDt(), "Candidate Timer");
        timer.setTimeoutListener(this);
        timer.start();
        voteMap = new HashMap<Integer, Integer>();
        sizeOfCluster = server.getEdgeMonitor().getAliveNodes() + 1;
        requiredVotes = Math.round((sizeOfCluster / 2) + 0.5f);
    }

    @Override
    public void vote(Work.WorkMessage message, Channel channel) {
        Leaderelection.RequestVote requestVote = message.getRequestVote();
        if (server.getCurrentTerm() < requestVote.getCurrentTerm()) {
            Vote vote = new Vote(nodeID, server.getCurrentTerm(), requestVote.getCandidateID());
            channel.writeAndFlush(vote.getMessage());
            voteMap.clear();
            timer.cancel();
            server.setCurrentState(new FollowerState(server));
        }
    }

    @Override
    public void startElection(int currentTerm, int candidateID) {
        voteMap.put(nodeID, currentTerm);
        RequestVote requestVote = new RequestVote(nodeID, currentTerm, candidateID);
        server.getEdgeMonitor().sendMessage(requestVote.getMessage());
    }


    @Override
    public void onHeartBeat(Work.WorkMessage workMessage, Channel channel) {
        logger.info("Heart Beat coming");
        timer.cancel();
        int receivedLeaderId = workMessage.getHeader().getNodeId();
        int receivedCurrentTerm = workMessage.getBeat().getCurrentTerm();
        int currentTerm = server.getCurrentTerm();
        //If current term is less than received. Revert back to Follower State.
        if (receivedCurrentTerm >= currentTerm) {
            server.setCurrentLeader(receivedLeaderId);
            server.setCurrentTerm(receivedCurrentTerm);
            timer.cancel();
            server.setCurrentState(new FollowerState(server));
        }
    }

    @Override
    public void onVoteReceived(Work.WorkMessage workMessage, Channel channel) {
        logger.info("Vote Received");
        int voterId = workMessage.getVote().getVoterID();
        int receivedCurrentTerm = workMessage.getVote().getCurrentTerm();
        voteMap.put(voterId, receivedCurrentTerm);
        if (voteMap.size() >= requiredVotes) {
            becomeLeader();
        }
    }

    @Override
    public void onClusterSizeChanged(int sizeOfCluster) {
        logger.info("Size Changed " + sizeOfCluster);
        this.sizeOfCluster = sizeOfCluster + 1;
        requiredVotes = Math.round((sizeOfCluster / 2) + 0.5f);
    }

    @Override
    public void onDutyReceived(Pipe.CommandMessage commandMessage, Channel channel) {
        LeaderStatus leaderStatus = new LeaderStatus(nodeID, Leaderelection.StatusOfLeader.LeaderState.LEADER_NOTKNOWN, Leaderelection.StatusOfLeader.AskLeader.LEADERIS, -1);
        channel.writeAndFlush(leaderStatus.getMessage());
    }

    @Override
    public void onGlobalDutyReceived(Global.GlobalMessage globalMessage, Channel channel) {

    }

    @Override
    public void onExternalWriteRequest(WorkMessage workMessage, Channel channel) {
        if (workMessage.getDuty().getBlockNo() == 0) {
            List<String> idList = CouchDBHandler.getids(workMessage.getDuty().getFilename());
            logger.info(idList.toString());
            if (!idList.isEmpty()) {
                logger.info("File already exists in External DB");
                fileExists = true;
            } else {
                fileExists = false;
            }
        }

        if (!fileExists) {
            logger.info("writing");
            Common.Duty duty = workMessage.getDuty();
            CouchDBHandler.addFile(duty.getFilename(), duty.getBlockData(), duty.getNumOfBlocks(),
                    duty.getBlockNo());
        }
        WriteCompleteMessage writeCompleteMessage = new WriteCompleteMessage(server.getConf().getNodeId(), workMessage);
        AdministerQueue.getInstance().enqueueOutgoingWork(writeCompleteMessage.getMessage(), channel);
    }

    @Override
    public void onInternalWriteRequest(WorkMessage workMessage, Channel channel) {
        String fileName = workMessage.getDuty().getFilename();
        byte[] fileData = null;
        try {
            fileData = RiakClientHandler.getInstance().getFile(fileName);
            logger.info("File Already exists");
        } catch (Exception e) {
            logger.info("Writing file");
        }
        if (fileData == null) {
            RiakClientHandler.getInstance().saveFile(workMessage.getDuty().getFilename(),
                    workMessage.getDuty().getBlockData().toByteArray());
            WriteCompleteMessage writeCompleteMessage = new WriteCompleteMessage(server.getConf().getNodeId(), workMessage);
            AdministerQueue.getInstance().enqueueOutgoingWork(writeCompleteMessage.getMessage(), channel);
        }
    }


    @Override
    public void onExternalReadRequest(WorkMessage workMessage, Channel channel) {
    	String fileName = workMessage.getDuty().getFilename();
        List<String> idList = CouchDBHandler.getids(fileName);
        int i = 1;
        for (String id : idList) {
            ByteString filesData = CouchDBHandler.getFile(id);

            ReadCompleteMessage readCompleteMessage = new ReadCompleteMessage(server.getConf().getNodeId(),
                    fileName, filesData, i++, idList.size(), workMessage.getDuty().getSender(),
                    workMessage.getDuty().getRequestId());

            logger.info("Printing external db message ");
            int leaderId = server.getCurrentLeader();
            Channel leaderChannel = EdgeMonitor.getEdges().getNode(leaderId).getChannel();

            AdministerQueue.getInstance().enqueueOutgoingWork(readCompleteMessage.getMessage(), leaderChannel);
        }
    }


    @Override
    public void onInternalReadRequest(WorkMessage workMessage, Channel channel) {
        String fileName = workMessage.getDuty().getFilename();
        byte[] fileData = new byte[0];
        try {
            fileData = RiakClientHandler.getInstance().getFile(fileName);
            // Send the in memory file to the leader as a work message
            ReadCompleteMessage readCompleteMessage = new ReadCompleteMessage(server.getConf().getNodeId(),
                    fileName, fileData, 1, workMessage.getDuty().getSender(),
                    workMessage.getDuty().getRequestId());
            int leaderId = server.getCurrentLeader();
            Channel leaderChannel = EdgeMonitor.getEdges().getNode(leaderId).getChannel();
            // Pass the slave work message to leader channel
            AdministerQueue.getInstance().enqueueOutgoingWork(readCompleteMessage.getMessage(), leaderChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInternalUpdateRequest(WorkMessage workMessage, Channel channel) {
        try {
            logger.info("Updating file");
            RiakClientHandler.getInstance().saveFile(workMessage.getDuty().getFilename(),
                    workMessage.getDuty().getBlockData().toByteArray());
        } catch (Exception e) {
            logger.info("File does not exist. Can't update");
        }
    }

    @Override
    public void onInternalDeleteRequest(WorkMessage workMessage, Channel channel) {
        String fileName = workMessage.getDuty().getFilename();
        try {
            RiakClientHandler.getInstance().deleteFile(fileName);
        } catch (Exception e) {
            logger.info("Cannot delete file does not exist");
        }
    }
    @Override
    public void onExternalUpdateRequest(Work.WorkMessage workMessage, Channel channel) {
        List<String> idList = CouchDBHandler.getids(workMessage.getDuty().getFilename());
        logger.info(idList.toString());
        if (!idList.isEmpty()) {
            Common.Duty duty = workMessage.getDuty();
            CouchDBHandler.updateFile(duty.getFilename(), duty.getBlockData(), duty.getNumOfBlocks(),
                    duty.getBlockNo());
        } else {
            logger.info("File already exists in External DB");

        }
	}

    @Override
    public void onExternalDeleteRequest(WorkMessage workMessage, Channel channel) {
    	try {
			CouchDBHandler.deletefilewithname(workMessage.getDuty().getFilename());
			logger.info("File "+workMessage.getDuty().getFilename()+"has been Deleted");

			} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.info("Cannot Delete");
		}
    }

    @Override
    public void onReadComplete(WorkMessage workMessage, Channel channel) {
        //Nothing to do
    }

    @Override
    public void onWriteComplete(WorkMessage workMessage, Channel channel) {
        //Nothing to do.
    }

    @Override
    public void onFileNotFound(WorkMessage workMessage, Channel channel) {
        ReadIncompleteMessage message = new ReadIncompleteMessage(nodeID, workMessage);
        channel.writeAndFlush(message.getMessage());
    }

    @Override
    public void onGlobalResponseReceived(Global.GlobalMessage globalMessage, Channel channel) {

    }


    @Override
    public void onElectionTimeout() {
        //Get Alive Nodes
        sizeOfCluster = server.getEdgeMonitor().getAliveNodes() + 1;
        requiredVotes = Math.round((sizeOfCluster / 2) + 0.5f);

        if (requiredVotes > 1) {
            timer.cancel();
            server.incrementCurrentTerm();
            this.startElection(server.getCurrentTerm(), nodeID);
            int electionTimeout = server.getConf().getHeartbeatDt() + (150 + new Random().nextInt(150));
            timer.start(electionTimeout);
            voteMap.clear();
        } else {
            becomeLeader();
        }

    }

    private void becomeLeader() {
        logger.info("###############################");
        logger.info("Size of the network is: " + sizeOfCluster);
        logger.info("Required vote count is: " + requiredVotes);
        logger.info("Votes received are: " + voteMap.size());
        logger.info("Time: " + System.currentTimeMillis());
        logger.info("###############################");
        timer.cancel();
        voteMap.clear();
        server.setCurrentLeader(nodeID);
        server.setCurrentState(new LeaderState(server));
    }
}
