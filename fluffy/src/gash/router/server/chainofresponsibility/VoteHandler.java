package gash.router.server.chainofresponsibility;

import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work;
import routing.Pipe;

/**
 * Processes the votes from different servers.
 *
 
 */
public class VoteHandler extends Handler {
    protected static Logger logger = LoggerFactory.getLogger(VoteHandler.class);

    public VoteHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(Work.WorkMessage message, Channel channel) {
        if (message.hasVote()) {
            logger.info("node " + message.getHeader().getNodeId() + "voted");
            server.onVoteReceived(message, channel);

        } else {
            next.processWorkMessage(message, channel);
        }
    }

    @Override
    public void processCommandMessage(Pipe.CommandMessage message, Channel channel) {

    }

    @Override
    public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {

    }


}
