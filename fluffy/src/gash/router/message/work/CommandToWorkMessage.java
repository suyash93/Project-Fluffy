package gash.router.message.work;

import pipe.work.Work;
import routing.Pipe.CommandMessage;

public class CommandToWorkMessage extends WorkMessage {

    public CommandToWorkMessage(int nodeId, CommandMessage cmdMsg) {
        super(nodeId);
        workBuilder.setDuty(cmdMsg.getDuty());
    }

    @Override
    public Work.WorkMessage getMessage() {
        workBuilder.setIsProcessed(false);
        return workBuilder.build();
    }

}
