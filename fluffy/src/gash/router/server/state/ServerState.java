package gash.router.server.state;

import gash.router.server.MessageServer;
import gash.router.server.chainofresponsibility.Handler;
import global.Global;
import io.netty.channel.Channel;
import pipe.work.Work;
import routing.Pipe;

/**
 * Abstract for State of the Server
 *

 */
public abstract class ServerState {
    protected MessageServer server;
    protected Handler handler;

    public ServerState(MessageServer server) {
        this.server = server;
    }

    /**
     * vote to candidate.
     *
     * @param workMessage Request Vote message from Candidate
     * @param channel     Channel between Candidate and voter edge.
     */
    public abstract void vote(Work.WorkMessage workMessage, Channel channel);

    /**
     * ask for vote.
     */
    public abstract void startElection(int currentTerm, int candidateId);

    /**
     * HeartBeat Received from Leader
     *
     * @param workMessage HeartBeat message from Leader
     * @param channel     Channel between Leader and Node.
     */
    public abstract void onHeartBeat(Work.WorkMessage workMessage, Channel channel);

    /**
     * Vote Received from Other Nodes.
     *
     * @param workMessage Vote message from Other Nodes.
     * @param channel     Channel between Candidate and Voter Node.
     */
    public abstract void onVoteReceived(Work.WorkMessage workMessage, Channel channel);

    public abstract void onClusterSizeChanged(int sizeOfCluster);

    public abstract void onDutyReceived(Pipe.CommandMessage commandMessage, Channel channel);

    public abstract void onGlobalDutyReceived(Global.GlobalMessage globalMessage, Channel channel);

    public abstract void onExternalWriteRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onInternalWriteRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onExternalReadRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onInternalReadRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onInternalUpdateRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onInternalDeleteRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onExternalUpdateRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onExternalDeleteRequest(Work.WorkMessage workMessage, Channel channel);

    public abstract void onReadComplete(Work.WorkMessage workMessage, Channel channel);

    public abstract void onWriteComplete(Work.WorkMessage workMessage, Channel channel);

    public abstract void onFileNotFound(Work.WorkMessage workMessage, Channel channel);

    public abstract void onGlobalResponseReceived(Global.GlobalMessage globalMessage, Channel channel);


}
