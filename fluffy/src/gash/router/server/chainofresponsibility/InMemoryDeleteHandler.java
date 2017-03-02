package gash.router.server.chainofresponsibility;

import gash.router.inmemory.RiakClientHandler;
import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.work.Work;
import routing.Pipe;


public class InMemoryDeleteHandler extends Handler {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryDeleteHandler.class);

    public InMemoryDeleteHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(Work.WorkMessage message, Channel channel) {
        logger.info("Inside INMemoryDelete");
        if (message.getDuty().getDutyType() == Common.Duty.DutyType.DELETEFILE && !message.getIsProcessed()) {
            String fileName = message.getDuty().getFilename();
            logger.info("File name is" + fileName);
            try {
                byte[] fileData = RiakClientHandler.getInstance().getFile(fileName);
                if (fileData != null) {
                    server.onInMemoryDeleteRequest(message, channel);
                }
            } catch (Exception e) {
                logger.info("File not found in Riak");
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
