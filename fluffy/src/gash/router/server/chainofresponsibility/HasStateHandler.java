package gash.router.server.chainofresponsibility;


import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import routing.Pipe;

public class HasStateHandler extends Handler {


    protected static Logger logger = LoggerFactory.getLogger("LeaderIs");

    public HasStateHandler(MessageServer server) {
        super(server);
    }

    public void processWorkMessage(WorkMessage msg, Channel channel) {
        if (msg.hasState()) {
            WorkState s = msg.getState();
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
