package gash.router.server.chainofresponsibility;

import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

public class ReadDoneHandler extends Handler {

    protected static Logger logger = LoggerFactory.getLogger(ReadDoneHandler.class);

    public ReadDoneHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(WorkMessage message, Channel channel) {
        if (message.getDuty().getDutyType() == Common.Duty.DutyType.GETFILE && message.getIsProcessed()) {
            try {
                //Message from a slave which has sent some data to be sent to the client.
                logger.info("Slave returned some data to be forwarded to client ");
                server.onReadComplete(message, channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            next.processWorkMessage(message, channel);
        }
    }

    @Override
    public void processCommandMessage(CommandMessage message, Channel channel) {

    }

    @Override
    public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {

    }


}
