package gash.router.server;

import gash.router.server.chainofresponsibility.*;
import gash.router.server.model.WorkMessageChannelGroup;
import gash.router.server.utilities.SupportMessageGenerator;
import io.netty.channel.Channel;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;

/**
 * Incoming Worker

 */
public class IncomingWorker extends MessageObserver implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(IncomingWorker.class);

	private AdministerQueue administratorSubject;
	private Handler handler;
	private MessageServer msgServer;
	private Timer timer;

	public IncomingWorker(MessageServer msgServer) {
		super();
		administratorSubject = AdministerQueue.getInstance();
		administratorSubject.setWorkerIncomingListener(this);
		this.msgServer = msgServer;
		handler = new ExternalWriteHandler(msgServer);

		// Init InMemory Handlers
		InMemoryWriteHandler inMemoryWriteHandler = new InMemoryWriteHandler(msgServer);
		InMemoryReadHandler inMemoryReadHandler = new InMemoryReadHandler(msgServer);
		InMemoryUpdateHandler inMemoryUpdateHandler = new InMemoryUpdateHandler(msgServer);
		InMemoryDeleteHandler inMemoryDeleteHandler = new InMemoryDeleteHandler(msgServer);

		// Init External Memory Handlers
		ExternalReadHandler externalReadHandler = new ExternalReadHandler(msgServer);
		ExternalDeleteHandler externalDeleteHandler = new ExternalDeleteHandler(msgServer);
		ExternalUpdateHandler externalUpdateHandler = new ExternalUpdateHandler(msgServer);
		ReadDoneHandler readDoneHandler = new ReadDoneHandler(msgServer);
		WrittenDoneHandler writtenDoneHandler = new WrittenDoneHandler(msgServer);
		FileNotFoundHandler fileNotFoundHandler = new FileNotFoundHandler(msgServer);

		// Setting next handlers.

		handler.setNext(inMemoryReadHandler);
		inMemoryReadHandler.setNext(inMemoryWriteHandler);
		inMemoryWriteHandler.setNext(inMemoryUpdateHandler);
		inMemoryUpdateHandler.setNext(inMemoryDeleteHandler);
		inMemoryDeleteHandler.setNext(readDoneHandler);
		readDoneHandler.setNext(writtenDoneHandler);
		writtenDoneHandler.setNext(externalReadHandler);
		externalReadHandler.setNext(externalDeleteHandler);
		externalDeleteHandler.setNext(externalUpdateHandler);
		externalUpdateHandler.setNext(fileNotFoundHandler);

		/*
		 * handler = new WorkStealHandler(msgServer);
		 */

		if (AdministerQueue.incomingWorkQueue == null)
			throw new RuntimeException("Poller has a null queue");

		/** Starting Work Stealing thread timer **/

		logger.info("############Starting Work Stealing thread#################");

		timer = new Timer();
		// timer.schedule(new RemindWorkSteal(), 1000);

	}

	@Override
	public void update() {
		try {
			// Poll the queue for messages
			logger.info("Inside update of Incoming Worker");
            WorkMessageChannelGroup wmCombo = administratorSubject.dequeueIncomingWork();
			WorkMessage currentWork = wmCombo.getWorkMessage();
			Channel currentChannel = wmCombo.getChannel();
			handler.processWorkMessage(currentWork, currentChannel);

			// TODO WORK STEAL REFACTORING
			/*
			 * if (AdministerQueue.incomingWorkQueue.isEmpty()) { logger.
			 * info("My incoming work queue is empty i am trying to steal work "
			 * ); AdministerQueue.notifyIncomingWorkerForSteal(); }
			 */

		} catch (InterruptedException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public void updateWorkSteal() {

		// select a node for checking if it has pending tasks and
		// stealing them
		logger.info("Trying to steal work from peers");

		Channel stealChannel = NodeManager.getNextChannelForWorkStealing();
		logger.info("Printing stealChannel from neighbour------" + stealChannel);
		if (stealChannel != null) {
			// Generate a Work steal request message to be sent to
			// the selected node
			WorkMessage workStealReqMessage = SupportMessageGenerator.getMessageInstance().produceWorkStealingMessage();
			// Enqueue the generated message to the outbound work
			// queue

			administratorSubject.enqueueOutgoingWork(workStealReqMessage, stealChannel);
			logger.info("Sending work steal request message");
		}

	}

	// Observer listening to the AdministerQueue subject

	@Override
	public void run() {

		/*
		 * *Checking if my incoming worker queue is empty*
		 */

		if (AdministerQueue.incomingWorkQueue.isEmpty()) {
			logger.info("My incoming work queue is empty i am trying to steal work ");

			AdministerQueue.notifyIncomingWorkerForSteal();

		}

	}

}
