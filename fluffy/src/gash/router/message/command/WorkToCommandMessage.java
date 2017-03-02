package gash.router.message.command;

import pipe.work.Work.WorkMessage;

public class WorkToCommandMessage extends CommandMessage {

    public WorkToCommandMessage(int nodeId, WorkMessage wrkMsg) {
        super(nodeId);

        commandBuilder.setDuty(wrkMsg.getDuty());

    }

    @Override
    public routing.Pipe.CommandMessage getMessage() {
        return commandBuilder.build();
    }


}