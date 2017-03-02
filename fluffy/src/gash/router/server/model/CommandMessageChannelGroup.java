package gash.router.server.model;

import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

public class CommandMessageChannelGroup {

    private Channel channel;
    private CommandMessage commandMessage;

    public CommandMessageChannelGroup(CommandMessage commandMessage, Channel channel) {
        super();
        this.channel = channel;
        this.commandMessage = commandMessage;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public CommandMessage getCommandMessage() {
        return this.commandMessage;
    }

}
