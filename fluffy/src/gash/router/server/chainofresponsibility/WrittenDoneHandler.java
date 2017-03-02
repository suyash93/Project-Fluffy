package gash.router.server.chainofresponsibility;

import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import routing.Pipe;

public class WrittenDoneHandler extends Handler {

    protected static Logger logger = LoggerFactory.getLogger(WrittenDoneHandler.class);

    public WrittenDoneHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(WorkMessage message, Channel channel) {
        if (message.getDuty().getDutyType() == Common.Duty.DutyType.SAVEFILE && message.getIsProcessed()) {
            server.onWriteComplete(message, channel);
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
