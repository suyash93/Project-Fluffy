package gash.router.message.command;

import routing.Pipe;


public class FileNotFoundMessage extends CommandMessage {

    public FileNotFoundMessage(int nodeId) {
        super(nodeId);
    }

    @Override
    public Pipe.CommandMessage getMessage() {
        commandBuilder.setMessage("File not Found in Cluster");
        return commandBuilder.build();
    }
}
