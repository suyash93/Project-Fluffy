package gash.router.message.command;

import com.google.protobuf.ByteString;

import gash.router.server.MessageServerImpl;
import pipe.common.Common.Duty;
import pipe.common.Common.Header;


public class InMemoryCommandMessage extends CommandMessage{
	private Duty.Builder db;

	public InMemoryCommandMessage(int nodeId,String filename,byte[] fileBytes) {
		
		super(nodeId);
		// TODO Auto-generated constructor stub
		db = Duty.newBuilder();
		db.setBlockNo(1);
		db.setNumOfBlocks(1);
		db.setBlockData(ByteString.copyFrom(fileBytes));
		db.setFilename(filename);
		//db.setDutyType(DutyType.GETFILE);
		db.setSender("Server");
		
		
	}

	@Override
	public routing.Pipe.CommandMessage getMessage() {
		// TODO Auto-generated method stub
		commandBuilder.setDuty(db);
	return commandBuilder.build();
		
		
	}
	
	

}
