package gash.router.server.chainofresponsibility;


import gash.router.global.edges.GlobalEdgeMonitor;
import gash.router.message.global.PingMessage;
import gash.router.server.MessageServer;
import gash.router.server.state.LeaderState;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;
import routing.Pipe;

public class PingHandler extends Handler {
    protected static Logger logger = LoggerFactory.getLogger(PingHandler.class);

    public PingHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(WorkMessage msg, Channel channel) {
        if (msg.hasPing()) {
            logger.info("ping from " + msg.getHeader().getNodeId());

        } else {
            next.processWorkMessage(msg, channel);
        }
    }

    @Override
    public void processCommandMessage(Pipe.CommandMessage message, Channel channel) {
        if (message.hasPing()) {
            logger.info("ping from client " + message.getHeader().getNodeId());
            int clusterId = server.getGlobalConf().getClusterId();

            PingMessage pingMessage = new PingMessage(clusterId, clusterId);
            try {
                logger.info(pingMessage.toString());
                GlobalEdgeMonitor.sendMessage(pingMessage.getMessage());
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            next.processCommandMessage(message, channel);
        }
    }

    @Override
    public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {
        if (message.hasPing()) {
            if (server.getCurrentState() instanceof LeaderState) {
                if (!(message.getGlobalHeader().getDestinationId() == server.getGlobalConf().getClusterId())) {
                    logger.info("Message Ping coming from " + message.getGlobalHeader().getClusterId());
                    int triggerId = server.getConf().getNodeId();
                    PingMessage pingMessage = new PingMessage(triggerId, message.getGlobalHeader().getDestinationId());
                    try {
                        GlobalEdgeMonitor.sendMessage(pingMessage.getMessage());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.info("Ping Successful");
                }
            }
        } else {
            next.processGlobalMessage(message, channel);
        }
    }


}
