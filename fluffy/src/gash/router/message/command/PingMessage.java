package gash.router.message.command;

import routing.Pipe;

public class PingMessage extends CommandMessage {

    public PingMessage(int nodeId) {
        super(nodeId);
    }

    @Override
    public Pipe.CommandMessage getMessage() {
        commandBuilder.setPing(true);
        return commandBuilder.build();
    }
}
