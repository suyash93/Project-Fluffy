package gash.router.message.command;

import com.google.protobuf.ByteString;

import pipe.common.Common.Duty;
import pipe.common.Common.Duty.DutyType;

public class ExternalDBCommandMessage extends CommandMessage {
	private Duty.Builder db;

	public ExternalDBCommandMessage(int nodeId, String filename, ByteString filesData, int blockNum, int numOfBlocks,
			String sender) {

		super(nodeId);
		// TODO Auto-generated constructor stub
		db = Duty.newBuilder();

		db.setBlockNo(blockNum);
		db.setBlockData(filesData);
		db.setNumOfBlocks(numOfBlocks);
		
		db.setFilename(filename);
		db.setSender("Server");

	}

	@Override
	public routing.Pipe.CommandMessage getMessage() {
		
		commandBuilder.setDuty(db);

		return commandBuilder.build();

	}

}
