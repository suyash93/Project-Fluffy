package gash.router.server.chainofresponsibility;

import gash.router.persistence.CouchDBHandler;
import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.work.Work;
import routing.Pipe;

import java.util.List;


public class ExternalReadHandler extends Handler {
    private static final Logger logger = LoggerFactory.getLogger(ExternalReadHandler.class);

    public ExternalReadHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(Work.WorkMessage message, Channel channel) {
        logger.info("Inside External Reader");
        if (message.getDuty().getDutyType() == Common.Duty.DutyType.GETFILE && !message.getIsProcessed()) {
            List<String> idList = CouchDBHandler.getids(message.getDuty().getFilename());
            logger.info(idList.toString());
            if (!idList.isEmpty()) {
                server.onExternalReadRequest(message, channel);
            } else {
                next.processWorkMessage(message, channel);
            }

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
