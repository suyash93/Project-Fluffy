package gash.router.message.global;

import com.google.protobuf.ByteString;
import global.Global;
import pipe.work.Work;


public class ReadResponseMessage extends GlobalMessage {
    Global.Response.Builder responseBuilder;
    Global.File.Builder fileBuilder;

    public ReadResponseMessage(int clusterId, int destinationId, String requestId, String fileName, int noOfChunks, int chunkId, ByteString filesData) {
        super(clusterId, destinationId);
        responseBuilder = Global.Response.newBuilder();
        fileBuilder = Global.File.newBuilder();
        fileBuilder.setFilename(fileName);
        fileBuilder.setData(filesData);
        fileBuilder.setChunkId(chunkId);
        fileBuilder.setTotalNoOfChunks(noOfChunks);


        responseBuilder.setFile(fileBuilder);
        responseBuilder.setRequestId(requestId);
        responseBuilder.setRequestType(Global.RequestType.READ);
        responseBuilder.setSuccess(true);

    }

    public ReadResponseMessage(int clusterId, int destinationId, Work.WorkMessage workMessage) {
        super(clusterId, destinationId);
        responseBuilder = Global.Response.newBuilder();
        fileBuilder = Global.File.newBuilder();
        fileBuilder.setFilename(workMessage.getDuty().getFilename());
        fileBuilder.setData(workMessage.getDuty().getBlockData());
        fileBuilder.setChunkId(workMessage.getDuty().getBlockNo());
        fileBuilder.setTotalNoOfChunks(workMessage.getDuty().getNumOfBlocks());


        responseBuilder.setFile(fileBuilder);
        responseBuilder.setRequestId(workMessage.getRequestId());
        responseBuilder.setRequestType(Global.RequestType.READ);
        responseBuilder.setSuccess(true);

    }

    public ReadResponseMessage(int clusterId, int desId, String requestId, String fileName, int noOfChunks, int chunkId,
			byte[] fileData) {
    	super(clusterId,desId);

    	responseBuilder = Global.Response.newBuilder();
        fileBuilder = Global.File.newBuilder();
        fileBuilder.setFilename(fileName);
        fileBuilder.setData(ByteString.copyFromUtf8(fileData.toString()));
        fileBuilder.setChunkId(chunkId);
        fileBuilder.setTotalNoOfChunks(noOfChunks);


        responseBuilder.setFile(fileBuilder);
        responseBuilder.setRequestId(requestId);
        responseBuilder.setRequestType(Global.RequestType.READ);
        responseBuilder.setSuccess(true);
	}

	@Override
    public Global.GlobalMessage getMessage() {
        globalMessageBuilder.setResponse(responseBuilder);
        return globalMessageBuilder.build();
    }
}
