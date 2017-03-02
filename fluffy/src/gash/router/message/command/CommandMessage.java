package gash.router.message.command;

import pipe.common.Common;
import routing.Pipe;


public abstract class CommandMessage {
    protected Pipe.CommandMessage.Builder commandBuilder;

    public CommandMessage(int nodeId) {
        Common.Header.Builder headerBuilder = Common.Header.newBuilder();
        headerBuilder.setNodeId(nodeId);
        headerBuilder.setDestination(-1);
        headerBuilder.setTime(System.currentTimeMillis());

        commandBuilder = Pipe.CommandMessage.newBuilder();
        commandBuilder.setHeader(headerBuilder);
    }

    public abstract Pipe.CommandMessage getMessage();
}
