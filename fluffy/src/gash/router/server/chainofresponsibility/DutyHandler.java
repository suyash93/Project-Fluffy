package gash.router.server.chainofresponsibility;

import gash.router.server.AdministerQueue;
import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work;
import routing.Pipe;


public class DutyHandler extends Handler {
    Logger logger = LoggerFactory.getLogger(DutyHandler.class);

    public DutyHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(Work.WorkMessage message, Channel channel) {
        if (message.hasDuty()) {
            AdministerQueue.getInstance().enqueueIncomingWork(message, channel);
        } else {
            next.processWorkMessage(message, channel);
        }
    }

    @Override
    public void processCommandMessage(Pipe.CommandMessage message, Channel channel) {
        if (message.hasDuty()) {
            server.onDutyMessage(message, channel);
        } else {
            next.processCommandMessage(message, channel);
        }
    }

    @Override
    public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {
        if (message.getGlobalHeader().getDestinationId() == server.getGlobalConf().getClusterId()) {
            logger.info("I got back my request");
        } else {
            if (message.hasRequest()) {
                server.onGlobalDutyMessage(message, channel);
            } else {
                next.processGlobalMessage(message, channel);
            }
        }

    }


}
