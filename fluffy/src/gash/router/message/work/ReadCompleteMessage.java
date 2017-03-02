package gash.router.message.work;

import com.google.protobuf.ByteString;
import pipe.common.Common;
import pipe.work.Work;


public class ReadCompleteMessage extends WorkMessage {

    private Common.Duty.Builder db;

    public ReadCompleteMessage(int nodeId, String filename, ByteString filesData, int blockNum, int numOfBlocks, String sender, String requestId) {
        super(nodeId);
        db = Common.Duty.newBuilder();
        db.setBlockNo(blockNum);
        db.setBlockData(filesData);
        db.setNumOfBlocks(numOfBlocks);
        db.setDutyType(Common.Duty.DutyType.GETFILE);
        db.setFilename(filename);
        db.setSender(sender);
        db.setRequestId(requestId);
        workBuilder.setDuty(db);
    }

    public ReadCompleteMessage(int nodeId, String filename, byte[] fileData, int numOfBlocks, String sender, String requestId) {
        super(nodeId);
        db = Common.Duty.newBuilder();
        db.setBlockData(ByteString.copyFromUtf8(fileData.toString()));
        db.setNumOfBlocks(numOfBlocks);
        db.setDutyType(Common.Duty.DutyType.GETFILE);
        db.setFilename(filename);
        db.setSender(sender);
        db.setRequestId(requestId);
        workBuilder.setDuty(db);
    }

    @Override
    public Work.WorkMessage getMessage() {

        workBuilder.setIsProcessed(true);
        return workBuilder.build();
    }


}
