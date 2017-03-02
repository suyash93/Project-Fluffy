package gash.router.server.chainofresponsibility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.MessageServer;
import global.Global.GlobalMessage;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

public class ExternalUpdateHandler extends Handler {

	private static final Logger logger = LoggerFactory.getLogger(ExternalUpdateHandler.class);

	public ExternalUpdateHandler(MessageServer server) {
		super(server);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void processWorkMessage(WorkMessage message, Channel channel) {
		// TODO Auto-generated method stub
		if (message.getDuty().getDutyType() == Common.Duty.DutyType.UPDATEFILE && !message.getIsProcessed()) {
			int blockCount = message.getDuty().getNumOfBlocks();
			logger.info("File has " + blockCount + " blocks");
			if (blockCount > 1) {
				server.onExternalUpdateRequest(message, channel);
			} else {
				next.processWorkMessage(message, channel);
			}
		} else {
			next.processWorkMessage(message, channel);
		}

	}

	@Override
	public void processCommandMessage(CommandMessage message, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processGlobalMessage(GlobalMessage message, Channel channel) {
		// TODO Auto-generated method stub

	}

}
