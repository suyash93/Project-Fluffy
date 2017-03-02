package gash.router.server.chainofresponsibility;

import gash.router.message.command.WorkToCommandMessage;
import gash.router.message.work.CommandToWorkMessage;
import gash.router.server.AdministerQueue;
import gash.router.server.MessageServer;
import gash.router.server.model.CommandMessageChannelGroup;
import global.Global;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common.Duty.DutyType;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkStealing;
import pipe.work.Work.WorkStealing.StealType;
import routing.Pipe;
import routing.Pipe.CommandMessage;

public class WorkStealHandler extends Handler {

	protected static Logger logger = LoggerFactory.getLogger(WorkStealHandler.class);

	public WorkStealHandler(MessageServer server) {
		super(server);
	}

	public void processWorkMessage(WorkMessage msg, Channel channel) {
		if (msg.hasSteal()) {
			AdministerQueue administerQueue = AdministerQueue.getInstance();
			switch (msg.getSteal().getStealtype()) {
			case STEAL_RESPONSE:

				logger.info("------Stealing work from node:------ " + msg.getHeader().getNodeId());
				/**
				 * Only work stealing may lead to enqueing of commands in
				 * follower's incoming command queue as otherwise it is always
				 * leader's queue
				 **/

				AdministerQueue.getInstance().enqueueIncomingWork(msg, channel);

				logger.info("------A task was stolen from another node------");

				break;

			case STEAL_REQUEST:

				// Give the first message in the incoming command queue to
				// stealer
				try {

					logger.info("Neighbour is asking to steal work from me!!!");

					CommandMessageChannelGroup cmdMsgOnQueue = AdministerQueue.getIncomingCommandQueue().peek();

					if (cmdMsgOnQueue != null
							&& cmdMsgOnQueue.getCommandMessage().getDuty().getDutyType() == DutyType.GETFILE) {
						logger.info("------Pending Read Request found in Queue, Sending to another node------");

						CommandMessage wmProto = administerQueue.dequeueIncomingCommand().getCommandMessage();
						CommandMessage.Builder cm = CommandMessage.newBuilder(wmProto);

						CommandToWorkMessage cmdWrkMsg = new CommandToWorkMessage(server.getConf().getNodeId(),
								cm.build());

						WorkMessage wrkMsg = cmdWrkMsg.getMessage();
						WorkMessage.Builder wm = WorkMessage.newBuilder(wrkMsg);
						WorkStealing.Builder stealMessage = WorkStealing.newBuilder();
						stealMessage.setStealtype(StealType.STEAL_RESPONSE);
						wm.setSteal(stealMessage);
						AdministerQueue.getInstance().enqueueOutgoingWork(wm.build(), channel);
					} else {
						logger.info("Sorry my queue is empty or i donot have any incoming Read requests");
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				break;

			default:
				break;
			}
		} else {
			next.processWorkMessage(msg, channel);
		}
	}

	@Override
	public void processCommandMessage(Pipe.CommandMessage message, Channel channel) {

	}

	@Override
	public void processGlobalMessage(Global.GlobalMessage message, Channel channel) {

	}
}
