package gash.router.server.chainofresponsibility;

import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Failure;
import pipe.work.Work.WorkMessage;
import routing.Pipe;


public class ErrorHandler extends Handler {

    protected static Logger logger = LoggerFactory.getLogger("LeaderIs");

    public ErrorHandler(MessageServer server) {
        super(server);
    }

    public void processWorkMessage(WorkMessage msg, Channel channel) {
        if (msg.hasErr()) {
            Failure err = msg.getErr();
            logger.error("failure from " + msg.getHeader().getNodeId());
        } else {
            next.processWorkMessage(msg, channel);
        }
    }

    @Override
    public void processCommandMessage(Pipe.CommandMessage message, Channel channel) {

    }

    @Override
    public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {

    }


}
