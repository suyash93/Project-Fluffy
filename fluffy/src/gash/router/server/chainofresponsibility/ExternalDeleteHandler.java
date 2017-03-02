package gash.router.server.chainofresponsibility;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gash.router.persistence.CouchDBHandler;
import gash.router.server.MessageServer;
import global.Global.GlobalMessage;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

public class ExternalDeleteHandler extends Handler {
	private static final Logger logger = LoggerFactory.getLogger(ExternalDeleteHandler.class);

	public ExternalDeleteHandler(MessageServer server) {
		super(server);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void processWorkMessage(WorkMessage message, Channel channel) {
		// TODO Auto-generated method stub
		logger.info("In External Memory Delete");
		if (message.getDuty().getDutyType() == Common.Duty.DutyType.DELETEFILE && !message.getIsProcessed()) {
			String fileName = message.getDuty().getFilename();
			logger.info("File name to be deleted is" + fileName);
			try {
				List<String> idList = CouchDBHandler.getids(message.getDuty().getFilename());
				logger.info(idList.toString());
				if (!idList.isEmpty()) {
					server.onExternalDeleteRequest(message, channel);
				}
			} catch (Exception e) {
				logger.info("File not found in Couch Db");
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
