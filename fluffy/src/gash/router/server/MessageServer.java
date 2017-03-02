package gash.router.server;

import gash.router.container.GlobalConf;
import gash.router.container.RoutingConf;
import gash.router.persistence.replication.DataReplicationManager;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.state.ServerState;
import global.Global;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import pipe.work.Work;
import routing.Pipe;

import java.util.HashMap;

/**
 * Interface for creating a message server
 *

 */
public interface MessageServer {

    void setCurrentState(ServerState serverState);

    ServerState getCurrentState();

    /**
     * vote to candidate.
     *
     * @param workMessage message requesting for vote.
     * @param channel     Channel between Candidate and Voter Edge
     */
    void vote(Work.WorkMessage workMessage, Channel channel);

    /**
     * ask for vote.
     */
    void askForVote(int currentTerm, int candidateId);


    public RoutingConf getConf();

    public void setConf(RoutingConf conf);

    public void incrementCurrentTerm();

    void setCurrentLeader(int leaderId);

    void setCurrentTerm(int currentTerm);

    int getCurrentLeader();

    int getCurrentTerm();

    void setEmon(EdgeMonitor emon);

    HashMap<Integer, ServerBootstrap> getBootstrap();

    GlobalConf getGlobalConf();

    EdgeMonitor getEdgeMonitor();

    NodeManager getNodeManager();

    DataReplicationManager getDataReplicationManager();

    /**
     * HeartBeat Received from Leader
     *
     * @param workMessage HeartBeat message from Leader
     * @param channel     Channel between Leader and Node.
     */
    void onHeartBeat(Work.WorkMessage workMessage, Channel channel);

    /**
     * Vote Received from Other Nodes.
     *
     * @param workMessage Vote message from Other Nodes.
     * @param channel     Channel between Candidate and Voter Node.
     */
    void onVoteReceived(Work.WorkMessage workMessage, Channel channel);

    void onClusterSizeChanged(int clusterSize);

    void onDutyMessage(Pipe.CommandMessage message, Channel channel);

    void onGlobalDutyMessage(Global.GlobalMessage message, Channel channel);

    void onInMemoryReadRequest(Work.WorkMessage message, Channel channel);

    void onExternalReadRequest(Work.WorkMessage message, Channel channel);

    void onInMemoryWriteRequest(Work.WorkMessage message, Channel channel);

    void onExternalWriteRequest(Work.WorkMessage message, Channel channel);

    void onInMemoryUpdateRequest(Work.WorkMessage message, Channel channel);

    void onInMemoryDeleteRequest(Work.WorkMessage message, Channel channel);

    void onExternalUpdateRequest(Work.WorkMessage message, Channel channel);

    void onExternalDeleteRequest(Work.WorkMessage message, Channel channel);

    void onReadComplete(Work.WorkMessage message, Channel channel);

    void onWriteComplete(Work.WorkMessage message, Channel channel);

    void onFileNotFound(Work.WorkMessage message, Channel channel);

    void onGlobalResponseRecevied(Global.GlobalMessage message, Channel channel);

}
