package gash.router.message.global;

import global.Global;
import routing.Pipe;


public abstract class GlobalMessage {
    protected Global.GlobalMessage.Builder globalMessageBuilder;

    public GlobalMessage(int clusterId, int destinationId) {
        Global.GlobalHeader.Builder headerBuilder = Global.GlobalHeader.newBuilder();
        headerBuilder.setClusterId(clusterId);
        headerBuilder.setDestinationId(destinationId);
        headerBuilder.setTime(System.currentTimeMillis());
        globalMessageBuilder = Global.GlobalMessage.newBuilder();
        globalMessageBuilder.setGlobalHeader(headerBuilder);
    }

    public abstract Global.GlobalMessage getMessage();


}
