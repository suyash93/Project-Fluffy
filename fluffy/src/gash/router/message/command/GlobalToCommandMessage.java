package gash.router.message.command;

import global.Global;
import pipe.common.Common;
import routing.Pipe;


public class GlobalToCommandMessage extends CommandMessage {
    private Common.Duty.Builder dutyBuilder;

    public GlobalToCommandMessage(int nodeId, Global.GlobalMessage globalMessage) {
        super(nodeId);
        dutyBuilder = Common.Duty.newBuilder();
        Global.Response response = globalMessage.getResponse();
        Global.File file = response.getFile();
        dutyBuilder.setBlockNo(file.getChunkId());
        dutyBuilder.setFilename(file.getFilename());
        dutyBuilder.setBlockData(file.getData());
        dutyBuilder.setNumOfBlocks(file.getTotalNoOfChunks());
    }

    @Override
    public Pipe.CommandMessage getMessage() {
        commandBuilder.setDuty(dutyBuilder);
        return commandBuilder.build();
    }
}
