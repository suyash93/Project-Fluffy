package gash.router.server.state;

import com.google.protobuf.ByteString;
import gash.router.inmemory.RiakClientHandler;
import gash.router.message.command.LeaderStatus;
import gash.router.message.work.ReadCompleteMessage;
import gash.router.message.work.ReadIncompleteMessage;
import gash.router.message.work.Vote;
import gash.router.message.work.WriteCompleteMessage;
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
import pipe.common.Common;
import pipe.leaderelection.Leaderelection;
import pipe.work.Work;
import pipe.work.Work.WorkMessage;
import routing.Pipe;

import java.util.List;
import java.util.Random;

/**
 * Follower State
 *

 */
public class FollowerState extends ServerState implements ElectionTimeoutListener {
    private static Logger logger = LoggerFactory.getLogger("Follower State");
    private Timer timer;
    private int nodeID;
    private boolean fileExists = false;
    // private Handler handler;

    public FollowerState(MessageServer server) {
        super(server);
        nodeID = server.getConf().getNodeId();
        int electionTimeout = server.getConf().getHeartbeatDt() + (150 + new Random().nextInt(150));
        // Start Timer for Election Timeout.
        timer = new Timer(electionTimeout, "Follower Timer");
        timer.setTimeoutListener(this);
        timer.start();
        logger.info("Follower");

    }

    @Override
    public void vote(Work.WorkMessage message, Channel channel) {
        Leaderelection.RequestVote requestVote = message.getRequestVote();
        if (server.getCurrentTerm() < requestVote.getCurrentTerm()) {
            Vote vote = new Vote(nodeID, server.getCurrentTerm(), requestVote.getCandidateID());
            channel.writeAndFlush(vote.getMessage());
            timer.cancel();
            server.setCurrentTerm(requestVote.getCurrentTerm());
            timer.start();
        }
    }

    @Override
    public void startElection(int currentTerm, int candidateID) {
        // Nothing to do.
    }

    @Override
    public void onHeartBeat(Work.WorkMessage workMessage, Channel channel) {
        // logger.info("Heart Beat coming");
        timer.cancel();
        int receivedLeaderId = workMessage.getHeader().getNodeId();
        int receivedCurrentTerm = workMessage.getBeat().getCurrentTerm();
        int currentTerm = server.getCurrentTerm();
        int currentLeaderId = server.getCurrentLeader();
        if (receivedCurrentTerm > currentTerm && receivedLeaderId != currentLeaderId) {
            server.setCurrentLeader(receivedLeaderId);
            server.setCurrentTerm(receivedCurrentTerm);
        }
        timer.start();
    }

    @Override
    public void onVoteReceived(Work.WorkMessage workMessage, Channel channel) {
        // Nothing to do
    }

    @Override
    public void onClusterSizeChanged(int clusterSize) {
        // Nothing to do.
    }

    @Override
    public void onDutyReceived(Pipe.CommandMessage commandMessage, Channel channel) {
        logger.info("Duty Received not LEader");
        LeaderStatus leaderStatus = new LeaderStatus(nodeID, Leaderelection.StatusOfLeader.LeaderState.LEADER_ALIVE,
                Leaderelection.StatusOfLeader.AskLeader.LEADERIS, server.getCurrentLeader());
        channel.writeAndFlush(leaderStatus.getMessage());
    }

    @Override
    public void onGlobalDutyReceived(Global.GlobalMessage globalMessage, Channel channel) {

    }

    @Override
    public void onExternalWriteRequest(WorkMessage workMessage, Channel channel) {
        logger.info("Writing block no " + workMessage.getDuty().getBlockNo());
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
            WriteCompleteMessage writeCompleteMessage = new WriteCompleteMessage(server.getConf().getNodeId(),
                    workMessage);
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

            ReadCompleteMessage readCompleteMessage = new ReadCompleteMessage(server.getConf().getNodeId(), fileName,
                    filesData, i++, idList.size(), workMessage.getDuty().getSender(),
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Send the in memory file to the leader as a work message
        ReadCompleteMessage readCompleteMessage = new ReadCompleteMessage(server.getConf().getNodeId(), fileName,
                fileData, 1, workMessage.getDuty().getSender(), workMessage.getDuty().getRequestId());
        int leaderId = server.getCurrentLeader();
        Channel leaderChannel = EdgeMonitor.getEdges().getNode(leaderId).getChannel();
        // Pass the slave work message to leader channel
        AdministerQueue.getInstance().enqueueOutgoingWork(readCompleteMessage.getMessage(), leaderChannel);
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
            logger.info("Deleting File");
            RiakClientHandler.getInstance().deleteFile(fileName);
        } catch (Exception e) {
            logger.info("Cannot delete file does not exist");
        }
    }

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
            logger.info("File " + workMessage.getDuty().getFilename() + "has been Deleted");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.info("Cannot Delete");
        }
    }

    @Override
    public void onReadComplete(WorkMessage workMessage, Channel channel) {
        // Nothing to do
    }

    @Override
    public void onWriteComplete(WorkMessage workMessage, Channel channel) {
        // Nothing to do
    }

    @Override
    public void onFileNotFound(WorkMessage workMessage, Channel channel) {
        logger.info("Replying to leader");
        ReadIncompleteMessage message = new ReadIncompleteMessage(nodeID, workMessage);
        channel.writeAndFlush(message.getMessage());
    }

    @Override
    public void onGlobalResponseReceived(Global.GlobalMessage globalMessage, Channel channel) {

    }

    @Override
    public void onElectionTimeout() {
        logger.info("Election Timed out");
        // Step 1: Increment Current Term
        server.incrementCurrentTerm();
        // Change Current State to Candidate State
        server.setCurrentState(new CandidateState(server));
        // Start Election
        server.askForVote(server.getCurrentTerm(), nodeID);

    }
}
