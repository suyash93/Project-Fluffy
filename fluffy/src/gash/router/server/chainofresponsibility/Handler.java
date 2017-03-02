package gash.router.server.chainofresponsibility;


import gash.router.server.AdministerQueue;
import gash.router.server.MessageServer;
import global.Global;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

public abstract class Handler {


    protected Handler next;

    protected MessageServer server;
    protected AdministerQueue administrator;

    public Handler(MessageServer server) {
        if (server != null) {
            this.server = server;
        }
    }

    public void setNext(Handler handler) {
        next = handler;
    }

    public abstract void processWorkMessage(WorkMessage message, Channel channel);

    public abstract void processCommandMessage(CommandMessage message, Channel channel);

    public abstract void processGlobalMessage(Global.GlobalMessage message, Channel channel);

}
