package gash.router.server.chainofresponsibility;

import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.work.Work;
import routing.Pipe;


public class InMemoryWriteHandler extends Handler {


    private static final Logger logger = LoggerFactory.getLogger(InMemoryWriteHandler.class);

    public InMemoryWriteHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(Work.WorkMessage message, Channel channel) {
        if (message.getDuty().getDutyType() == Common.Duty.DutyType.SAVEFILE && !message.getIsProcessed()) {
            int blockCount = message.getDuty().getNumOfBlocks();
            logger.info("File has " + blockCount + " blocks");
            if (message.getDuty().getNumOfBlocks() == 1) {
                server.onInMemoryWriteRequest(message, channel);
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
