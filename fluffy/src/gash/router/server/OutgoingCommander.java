package gash.router.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gash.router.server.model.CommandMessageChannelGroup;
import io.netty.channel.ChannelFuture;

/**
 * Outgoing Commander
 
 *
 */

public class OutgoingCommander extends MessageObserver {

	private AdministerQueue administratorSubject;
	protected static Logger logger = LoggerFactory.getLogger(OutgoingCommander.class);

	public OutgoingCommander(MessageServer server) {
		super();
		this.administratorSubject = AdministerQueue.getInstance();
		this.administratorSubject.setCommanderOutgoingListener(this);
		if (administratorSubject.outgoingCommandQueue == null)
			throw new RuntimeException(" Administrator has a null queue");
	}

	@Override
	public void update() {

		try {
			// block until a message is enqueued
			CommandMessageChannelGroup msg = AdministerQueue.getInstance().dequeueOutgoingCommmand();
			System.out.println("Writing from outgoing commander to client channel " + msg.getChannel());
			/**
			 * Incrementing the no. of processed tasks
			 */
			// NodeState.getInstance().incrementProcessed();

			//logger.info("Printing message to be written to the client");

			if (logger.isDebugEnabled())
				logger.debug("Outbound management message routing to node "
						+ msg.getCommandMessage().getHeader().getDestination());

			if (msg.getChannel() != null && msg.getChannel().isOpen()) {
				boolean rtn = false;
				//if (msg.getChannel().isWritable()) {

					logger.info("Channel is writable");

					// System.out.println("Channel is writable on outgoing
					// commander side . Lets inspect the command message to be
					// written");
					// System.out.println(msg.getCommandMessage());
					ChannelFuture cf = msg.getChannel().write(msg.getCommandMessage());
					logger.info("Block no "+msg.getCommandMessage().getDuty().getBlockNo()+" has been written on channnel");
				    msg.getChannel().flush();

					//cf.awaitUninterruptibly();

					//This was causing channel promise incomplete
					//rtn = cf.isSuccess();
					//if (!rtn)
						//administratorSubject.generateOutgoingCommand(msg);
				//}else{
					//logger.info("Channel not writable_------");
				//}

			} else {
				logger.info("Channel to client "+msg.getCommandMessage().getHeader().getDestination()+"is not write");
				logger.info("is channel null"+(msg.getChannel()==null));
				//administratorSubject.generateOutgoingCommand(msg);
			}

		} catch (InterruptedException ie) {

		} catch (Exception e) {
			logger.error("Unexpected management communcation failure", e);

		}

	}

	@Override
	public void updateWorkSteal() {
		// TODO Auto-generated method stub

	}

}
