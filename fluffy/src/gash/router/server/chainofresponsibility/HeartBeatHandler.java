package gash.router.server.chainofresponsibility;

import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work;
import routing.Pipe;


public class HeartBeatHandler extends Handler {

    protected static Logger logger = LoggerFactory.getLogger("HeartBeat");

    public HeartBeatHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(Work.WorkMessage message, Channel channel) {
        if (message.hasBeat()) {
            logger.info("heartbeat from " + message.getHeader().getNodeId());
            server.onHeartBeat(message, channel);
        } else {
            next.processWorkMessage(message, channel);
        }
    }

    @Override
    public void processCommandMessage(Pipe.CommandMessage message, Channel channel) {
        //Nothing to do.
    }

    @Override
    public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {

    }


}
