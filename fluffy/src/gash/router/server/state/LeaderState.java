package gash.router.server.state;

import gash.router.global.edges.GlobalEdgeMonitor;
import gash.router.inmemory.RiakClientHandler;
import gash.router.message.command.ExternalDBCommandMessage;
import gash.router.message.command.GlobalToCommandMessage;
import gash.router.message.command.InMemoryCommandMessage;
import gash.router.message.command.WorkToCommandMessage;
import gash.router.message.global.ReadResponseMessage;
import gash.router.message.global.WorkToGlobalMessage;
import gash.router.message.work.CommandToWorkMessage;
import gash.router.message.work.Vote;
import gash.router.persistence.CouchDBHandler;
import gash.router.persistence.replication.DataReplicationManager;
import gash.router.server.AdministerQueue;
import gash.router.server.MessageServer;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.model.WorkMessageChannelGroup;
import gash.router.server.utilities.PrintUtil;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import pipe.common.Common;
import pipe.leaderelection.Leaderelection;
import pipe.work.Work;
import routing.Pipe;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Leader State
 *

 */
public class LeaderState extends ServerState {

	private final Logger logger = LoggerFactory.getLogger("Leader");
	private int nodeId;
	private HeartBeatThread heartBeatThread;
	private boolean forever = true;
	private static int fileNotFoundCounter = 0;
	private GlobalEdgeMonitor globalEdgeMonitor;
	private int clusterId = 0;
	private boolean fileExists = false;

	public LeaderState(MessageServer server) {
		super(server);
		logger.info("I am leader for term " + server.getCurrentTerm());
		this.nodeId = server.getConf().getNodeId();
		this.clusterId = server.getGlobalConf().getClusterId();
		heartBeatThread = new HeartBeatThread();
		sendHeartBeat();
		globalEdgeMonitor = new GlobalEdgeMonitor(server);
		Thread globalThread = new Thread(globalEdgeMonitor);
		globalThread.start();
	}

	@Override
	public void vote(Work.WorkMessage message, Channel channel) {
		Leaderelection.RequestVote requestVote = message.getRequestVote();
		if (server.getCurrentTerm() < requestVote.getCurrentTerm()) {
			Vote vote = new Vote(nodeId, server.getCurrentTerm(), requestVote.getCandidateID());
			channel.writeAndFlush(vote.getMessage());
			forever = false;
			server.setCurrentState(new FollowerState(server));
		}
	}

	@Override
	public void startElection(int currentTerm, int candidateID) {
		// Nothing to do.
	}

	private void sendHeartBeat() {
		heartBeatThread.start();

	}

	@Override
	public void onHeartBeat(Work.WorkMessage workMessage, Channel channel) {
		logger.info("Heart Beat Received");
		int receivedLeaderId = workMessage.getHeader().getNodeId();
		int receivedCurrentTerm = workMessage.getBeat().getCurrentTerm();
		// If Another Leader with Greater Current Term sends the Heart beat,
		// node will go back to follower state.
		if (server.getCurrentTerm() < receivedCurrentTerm) {
			forever = false;
			server.setCurrentTerm(receivedCurrentTerm);
			server.setCurrentLeader(receivedLeaderId);
			server.setCurrentState(new FollowerState(server));
		} else if (server.getCurrentTerm() == receivedCurrentTerm) {
			forever = false;
			server.setCurrentState(new CandidateState(server));
		}
	}

	@Override
	public void onVoteReceived(Work.WorkMessage workMessage, Channel channel) {
		// Nothing to do.
	}

	@Override
	public void onClusterSizeChanged(int clusterSize) {
		// Nothing to do.
	}

	@Override
	public void onDutyReceived(Pipe.CommandMessage commandMessage, Channel channel) {
		logger.info("Received task from " + commandMessage.getHeader().getNodeId());
		CommandToWorkMessage workMessage = new CommandToWorkMessage(nodeId, commandMessage);
		String requestId = workMessage.getMessage().getDuty().getRequestId();
		logger.info("Request id " + requestId);
		WorkMessageChannelGroup entry = new WorkMessageChannelGroup(channel, workMessage.getMessage());
		server.getNodeManager().addClientChannelwithRequestId(entry, requestId);
		AdministerQueue.getInstance().enqueueIncomingWork(workMessage.getMessage(), channel);
	}

	@Override
	public void onGlobalDutyReceived(Global.GlobalMessage globalMessage, Channel channel) {
		logger.info("Received task from" + globalMessage.getGlobalHeader().getClusterId());
		logger.info("Got READ REQUEST FROM GLOBAL");
		logger.info(globalMessage.toString());

		Global.Request request = globalMessage.getRequest();
		String fileName = request.getFileName();
		logger.info("Global File Name is " + fileName);
		String requestId = request.getRequestId();
		Global.RequestType type = request.getRequestType();
		Common.Header.Builder hb = Common.Header.newBuilder();
		// prepare the Header Structure
		hb.setNodeId(globalMessage.getGlobalHeader().getClusterId());
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(server.getConf().getNodeId());

		Common.Duty.Builder db = Common.Duty.newBuilder();
		db.setDestId(globalMessage.getGlobalHeader().getDestinationId());
		switch (type) {
		case READ:
			db.setFilename(fileName);
			db.setDutyType(Common.Duty.DutyType.GETFILE);
			db.setRequestId(requestId);
			db.setIsGlobal(true);
			try {
				db.setSender(InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			break;
		case WRITE:
			db.setFilename(request.getFile().getFilename());
			db.setRequestId(requestId);
			db.setDutyType(Common.Duty.DutyType.SAVEFILE);
			db.setRequestId(requestId);
			db.setBlockData(request.getFile().getData());
			db.setNumOfBlocks(request.getFile().getTotalNoOfChunks());
			db.setBlockNo(request.getFile().getChunkId());
			db.setIsGlobal(true);
			try {
				db.setSender(InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			break;
		case DELETE:
			db.setFilename(fileName);
			db.setDutyType(Common.Duty.DutyType.DELETEFILE);
			db.setRequestId(requestId);
			db.setIsGlobal(true);
			try {
				db.setSender(InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			break;
		case UPDATE:
			db.setFilename(request.getFile().getFilename());
			db.setRequestId(requestId);
			db.setDutyType(Common.Duty.DutyType.UPDATEFILE);
			db.setRequestId(requestId);
			db.setBlockData(request.getFile().getData());
			db.setNumOfBlocks(request.getFile().getTotalNoOfChunks());
			db.setBlockNo(request.getFile().getChunkId());
			db.setIsGlobal(true);
			try {
				db.setSender(InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			break;
		}
		Work.WorkMessage.Builder workBuilder = Work.WorkMessage.newBuilder();
		workBuilder.setHeader(hb);
		workBuilder.setDuty(db);
		AdministerQueue.getInstance().enqueueIncomingWork(workBuilder.build(), channel);

	}

	@Override
	public void onExternalWriteRequest(Work.WorkMessage workMessage, Channel channel) {
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
			CouchDBHandler.addFile(duty.getFilename(), duty.getBlockData(), duty.getNumOfBlocks(), duty.getBlockNo());
			doReplication(workMessage);
		}
		if (!workMessage.getDuty().getIsGlobal()) {
			// TODO CLIENT ABOUT THE PROGRESS
			// TODO CREATE GLOBAL MESSAGE TO OTHER CLUSTERS
			logger.info("Sending file with chunk id " + workMessage.getDuty().getBlockNo());
			broadcastRequestGlobally(clusterId, workMessage);
		} else {
			broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
		}
	}

	@Override
	public void onInternalWriteRequest(Work.WorkMessage workMessage, Channel channel) {
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
			doReplication(workMessage);
		}

		if (!workMessage.getDuty().getIsGlobal()) {
			broadcastRequestGlobally(clusterId, workMessage);
		} else {
			broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
		}

	}

	@Override
	public void onExternalReadRequest(Work.WorkMessage workMessage, Channel channel) {
		String fileName = workMessage.getDuty().getFilename();
		List<String> idList = CouchDBHandler.getids(fileName);
		int i = 1;
		for (String id : idList) {
			ByteString filesData = CouchDBHandler.getFile(id);
			logger.info("Printing byteSize after getting from external" + filesData.size());
			ExternalDBCommandMessage externalMemoryMsg = new ExternalDBCommandMessage(server.getCurrentLeader(),
					fileName, filesData, i++, idList.size(), workMessage.getDuty().getSender());
			logger.info("Printing external db message ");
			logger.info(externalMemoryMsg.toString());
			logger.info("Global Message " + workMessage.getDuty().getIsGlobal());
			if (!workMessage.getDuty().getIsGlobal()) {
				PrintUtil.printCommand(externalMemoryMsg.getMessage());
				AdministerQueue.getInstance().enqueueOutgoingCommmand(externalMemoryMsg.getMessage(), channel);
			} else {
				int desId = workMessage.getDuty().getDestId();
				int clusterId = server.getGlobalConf().getClusterId();
				ReadResponseMessage readResponseMessage = new ReadResponseMessage(clusterId, desId,
						workMessage.getDuty().getRequestId(), fileName, 1, 0, filesData);
				logger.info(readResponseMessage.getMessage().toString());
				GlobalEdgeMonitor.sendMessage(readResponseMessage.getMessage());
			}

		}
	}

	@Override
	public void onInternalReadRequest(Work.WorkMessage workMessage, Channel channel) {
		String fileName = workMessage.getDuty().getFilename();
		byte[] fileData = new byte[0];
		try {
			fileData = RiakClientHandler.getInstance().getFile(fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		InMemoryCommandMessage inMemoryMsg = new InMemoryCommandMessage(server.getCurrentLeader(), fileName, fileData);
		// Enqueue to the outgoing commander to write to the channel
		logger.info("Global Message " + workMessage.getDuty().getIsGlobal());
		if (!workMessage.getDuty().getIsGlobal()) {
			PrintUtil.printCommand(inMemoryMsg.getMessage());
			AdministerQueue.getInstance().enqueueOutgoingCommmand(inMemoryMsg.getMessage(), channel);
		} else {
			int desId = workMessage.getDuty().getDestId();
			int clusterId = server.getGlobalConf().getClusterId();
			ReadResponseMessage readResponseMessage = new ReadResponseMessage(clusterId, desId,
					workMessage.getDuty().getRequestId(), fileName, 1, 0, fileData);
			logger.info(readResponseMessage.getMessage().toString());
			GlobalEdgeMonitor.sendMessage(readResponseMessage.getMessage());
		}

	}

	@Override
	public void onInternalUpdateRequest(Work.WorkMessage workMessage, Channel channel) {
		try {
			logger.info("Updating file");
			RiakClientHandler.getInstance().saveFile(workMessage.getDuty().getFilename(),
					workMessage.getDuty().getBlockData().toByteArray());
			server.getEdgeMonitor().sendMessage(workMessage);
		} catch (Exception e) {
			logger.info("File does not exist. Can't update");
		}
		if (!workMessage.getDuty().getIsGlobal()) {
			broadcastRequestGlobally(clusterId, workMessage);
		} else {
			broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
		}

	}

	@Override
	public void onInternalDeleteRequest(Work.WorkMessage workMessage, Channel channel) {
		String fileName = workMessage.getDuty().getFilename();
		try {
			logger.info("File deleted");
			RiakClientHandler.getInstance().deleteFile(fileName);
			server.getEdgeMonitor().sendMessage(workMessage);
		} catch (Exception e) {
			logger.info("Cannot delete file does not exist");
		}
		if (!workMessage.getDuty().getIsGlobal()) {
		} else {
			broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
		}

	}

	@Override
	public void onExternalUpdateRequest(Work.WorkMessage workMessage, Channel channel) {

		Common.Duty duty = workMessage.getDuty();
		String filename = workMessage.getDuty().getFilename();
		if (!CouchDBHandler.getids(filename).isEmpty()) {
			CouchDBHandler.updateFile(duty.getFilename(), duty.getBlockData(), duty.getNumOfBlocks(),
					duty.getBlockNo());

		}

		else {

			logger.info("File already exists hence");
		}
		server.getEdgeMonitor().sendMessage(workMessage);
		if (!workMessage.getDuty().getIsGlobal()) {
			broadcastRequestGlobally(clusterId, workMessage);
		} else {
			broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
		}

	}

	@Override
	public void onExternalDeleteRequest(Work.WorkMessage workMessage, Channel channel) {
		try {

			CouchDBHandler.deletefilewithname(workMessage.getDuty().getFilename());
			logger.info("File " + workMessage.getDuty().getFilename() + "has been Deleted");
			server.getEdgeMonitor().sendMessage(workMessage);
		} catch (Exception e) {
			logger.info("Cannot Delete");
		}
		if (!workMessage.getDuty().getIsGlobal()) {
			broadcastRequestGlobally(clusterId, workMessage);
		} else {
			broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
		}
	}

	@Override
	public void onReadComplete(Work.WorkMessage workMessage, Channel channel) {
		// Retrieving the client channel//
		String requestId = workMessage.getDuty().getRequestId();
		Channel clientChannel = server.getNodeManager().getClientChannelFromMap(requestId).getChannel();
		if (!workMessage.getFileNotFoundInNode()) {
			/**
			 * Converting the workMessage to commandMessage in order to be sent
			 * to the client
			 **/
			if (!workMessage.getDuty().getIsGlobal()) {
				logger.info("Sending to client on channel" + clientChannel);

				WorkToCommandMessage cmdMsg = new WorkToCommandMessage(server.getConf().getNodeId(), workMessage);
				AdministerQueue.getInstance().enqueueOutgoingCommmand(cmdMsg.getMessage(), clientChannel);
			} else {
				int desId = workMessage.getDuty().getDestId();
				int clusterId = server.getGlobalConf().getClusterId();
				ReadResponseMessage readResponseMessage = new ReadResponseMessage(clusterId, desId, workMessage);
				logger.info(readResponseMessage.getMessage().toString());
				GlobalEdgeMonitor.sendMessage(readResponseMessage.getMessage());
			}
		} else {
			fileNotFoundCounter++;
			logger.info("File Not Found in " + workMessage.getHeader().getNodeId());
			logger.info("Alive Edges are " + EdgeMonitor.aliveEdges.size());
			if (fileNotFoundCounter == EdgeMonitor.aliveEdges.size()) {
				logger.info("********Sending messaage to Global************");
				if (!workMessage.getDuty().getIsGlobal()) {
					broadcastRequestGlobally(clusterId, workMessage);
				} else {
					broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
				}
			}

		}

	}

	@Override
	public void onWriteComplete(Work.WorkMessage workMessage, Channel channel) {
		logger.info(" Slave Write complete!!");
	}

	@Override
	public void onFileNotFound(Work.WorkMessage workMessage, Channel channel) {
		if (EdgeMonitor.aliveEdges.size() == 0) {
			if (!workMessage.getDuty().getIsGlobal()) {
				broadcastRequestGlobally(clusterId, workMessage);
			} else {
				broadcastRequestGlobally(workMessage.getDuty().getDestId(), workMessage);
			}
		} else {
			logger.info("Asking others");
			server.getEdgeMonitor().sendMessage(workMessage);
		}

	}

	@Override
	public void onGlobalResponseReceived(Global.GlobalMessage globalMessage, Channel channel) {
		if (globalMessage.getGlobalHeader().getDestinationId() == server.getGlobalConf().getClusterId()) {
			String requestId = globalMessage.getRequest().getRequestId();
			Channel clientChannel = server.getNodeManager().getClientChannelFromMap(requestId).getChannel();
			GlobalToCommandMessage commandMessage = new GlobalToCommandMessage(nodeId, globalMessage);
			AdministerQueue.getInstance().enqueueOutgoingCommmand(commandMessage.getMessage(), clientChannel);
		} else {
			GlobalEdgeMonitor.sendMessage(globalMessage);
		}
	}

	private class HeartBeatThread extends Thread {
		@Override
		public void run() {
			super.run();

			while (forever) {
				try {
					server.getEdgeMonitor().sendMessage(createHB());
					synchronized (this) {
						wait(500);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Work.WorkMessage createHB() {
		Work.WorkState.Builder sb = Work.WorkState.newBuilder();
		sb.setEnqueued(-1);
		sb.setProcessed(-1);

		Work.Heartbeat.Builder bb = Work.Heartbeat.newBuilder();
		bb.setState(sb);
		bb.setCurrentTerm(server.getCurrentTerm());
		Common.Header.Builder hb = Common.Header.newBuilder();
		hb.setNodeId(server.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());
		Work.WorkMessage.Builder wb = Work.WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setSecret(12344);
		wb.setBeat(bb);
		return wb.build();
	}

	private void doReplication(Work.WorkMessage workMessage) {
		try {
			DataReplicationManager.getInstance().replicate(workMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void broadcastRequestGlobally(int destId, Work.WorkMessage workMessage) {
		WorkToGlobalMessage globalMessage = new WorkToGlobalMessage(clusterId, destId, workMessage);

		GlobalEdgeMonitor.sendMessage(globalMessage.getMessage());
	}

}
