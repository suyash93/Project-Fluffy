package gash.router.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.model.WorkMessageChannelGroup;
import io.netty.channel.ChannelFuture;

/**
 * Outgoing Worker
 
 *
 */

public class OutgoingWorker extends MessageObserver{

	private AdministerQueue administratorSubject;
	protected static Logger logger = LoggerFactory.getLogger(OutgoingWorker.class);

	public OutgoingWorker(MessageServer server) {
		super();
		this.administratorSubject = AdministerQueue.getInstance();
		this.administratorSubject.setWorkerOutgoingListenerr(this);
		if (administratorSubject.outgoingWorkQueue == null)
			throw new RuntimeException("Manager has a null queue");
	}



	@Override
	public void update() {


			try {
				// block until a message is enqueued
				logger.info("I am inside outgoing worker");

				WorkMessageChannelGroup msg = administratorSubject.dequeueOutgoingWork();

				logger.info(""+msg.getChannel());

				//NodeState.getInstance().incrementProcessed();
				//if (logger.isDebugEnabled())
					//logger.debug("Outbound management message routing to node " + msg.getWorkMessage().getHeader().getDestination());


				if (msg.getChannel()!= null && msg.getChannel().isOpen()) {
					logger.info("Channel not null");
					boolean rtn = false;
					MessageServerImpl.processed++;

					ChannelFuture cf = msg.getChannel().write(msg.getWorkMessage());
					msg.getChannel().flush();




				} else {
					logger.info("channel to node " + msg.getWorkMessage().getHeader().getDestination() + " is not writable");
					logger.info("Is channel null : "+(msg.getChannel() == null));
					//manager.returnOutboundWork(msg);
				}
			} catch (InterruptedException ie)
			{
				ie.printStackTrace();
			} catch (Exception e) {
				logger.error("Unexpected management communcation failure", e);

			}
		}



	@Override
	public void updateWorkSteal() {
		// TODO Auto-generated method stub

	}

}
