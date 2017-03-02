package gash.router.message.global;

import global.Global;
import pipe.common.Common;
import pipe.work.Work;


public class WorkToGlobalMessage extends GlobalMessage {
    private Work.WorkMessage workMessage;
    private Global.Request.Builder requestBuilder;

    public WorkToGlobalMessage(int clusterId, int destinationId, Work.WorkMessage workMessage) {
        super(clusterId, destinationId);
        this.workMessage = workMessage;
        requestBuilder = Global.Request.newBuilder();

    }

    @Override
    public Global.GlobalMessage getMessage() {
        if (workMessage.hasDuty()) {
            Common.Duty duty = workMessage.getDuty();
            String requestId = workMessage.getDuty().getRequestId();
            String fileName = workMessage.getDuty().getFilename();
            Global.File.Builder fileBuilder = Global.File.newBuilder();
            switch (duty.getDutyType()) {
                case GETFILE:
                    requestBuilder.setRequestId(requestId);
                    requestBuilder.setRequestType(Global.RequestType.READ);
                    requestBuilder.setFileName(fileName);
                    break;
                case SAVEFILE:
                    requestBuilder.setRequestId(requestId);
                    requestBuilder.setRequestType(Global.RequestType.WRITE);
                    fileBuilder.setFilename(fileName);
                    fileBuilder.setData(duty.getBlockData());
                    fileBuilder.setChunkId(duty.getBlockNo());
                    fileBuilder.setTotalNoOfChunks(duty.getNumOfBlocks());
                    requestBuilder.setFile(fileBuilder);
                    break;
                case UPDATEFILE:
                    requestBuilder.setRequestId(requestId);
                    requestBuilder.setRequestType(Global.RequestType.UPDATE);
                    fileBuilder.setFilename(fileName);
                    fileBuilder.setData(duty.getBlockData());
                    fileBuilder.setChunkId(duty.getBlockNo());
                    fileBuilder.setTotalNoOfChunks(duty.getNumOfBlocks());
                    requestBuilder.setFile(fileBuilder);
                    break;
                case DELETEFILE:
                    requestBuilder.setRequestId(requestId);
                    requestBuilder.setRequestType(Global.RequestType.DELETE);
                    requestBuilder.setFileName(fileName);
                    break;
            }
            globalMessageBuilder.setRequest(requestBuilder);
        }

        return globalMessageBuilder.build();
    }
}
