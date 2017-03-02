package gash.router.server.chainofresponsibility;

import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import pipe.work.Work;
import routing.Pipe;


public class GlobalResponseHandler extends Handler {

    public GlobalResponseHandler(MessageServer server) {
        super(server);
    }

    @Override
    public void processWorkMessage(Work.WorkMessage message, Channel channel) {

    }

    @Override
    public void processCommandMessage(Pipe.CommandMessage message, Channel channel) {

    }

    @Override
    public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {
        if (message.hasResponse()) {
            server.onGlobalResponseRecevied(message, channel);
        } else {
            next.processGlobalMessage(message, channel);
        }
    }
}
