package gash.router.server;

import gash.router.server.model.CommandMessageChannelGroup;
import gash.router.server.model.WorkMessageChannelGroup;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * For administering the Queue

 */

public class AdministerQueue {
    private static final Logger logger = LoggerFactory.getLogger(AdministerQueue.class);
    private List<MessageObserver> incomingCommanderListenersList = new ArrayList<MessageObserver>();
    private static List<MessageObserver> incomingWorkerListenersList = new ArrayList<MessageObserver>();
    private List<MessageObserver> outgoingCommanderListenersList = new ArrayList<MessageObserver>();
    private List<MessageObserver> outgoingWorkerListenersList = new ArrayList<MessageObserver>();


    protected static AtomicReference<AdministerQueue> instance = new AtomicReference<AdministerQueue>();
    protected static LinkedBlockingDeque<CommandMessageChannelGroup> incomingCommandQueue;
    protected LinkedBlockingDeque<CommandMessageChannelGroup> outgoingCommandQueue;
    protected static volatile LinkedBlockingDeque<WorkMessageChannelGroup> incomingWorkQueue;
    protected LinkedBlockingDeque<WorkMessageChannelGroup> outgoingWorkQueue;


    public static AdministerQueue initAdministration() {
        instance.compareAndSet(null, new AdministerQueue());
        return instance.get();
    }

    public static AdministerQueue getInstance() throws NullPointerException {
        return instance.get();
    }

    private AdministerQueue() {
        logger.info("Starting Queue Administration");

        logger.info("-----Initilializing Command Queues and Command Observers----");
        incomingCommandQueue = new LinkedBlockingDeque<CommandMessageChannelGroup>();
        outgoingCommandQueue = new LinkedBlockingDeque<CommandMessageChannelGroup>();


        logger.info("-----Initilializing Work Queues and Worker Observers----");
        incomingWorkQueue = new LinkedBlockingDeque<WorkMessageChannelGroup>();
        outgoingWorkQueue = new LinkedBlockingDeque<WorkMessageChannelGroup>();


        logger.info("-----Initilializing Common Cluster Queues and CommonCluster Observers Observers----");
    }

    /*
     * Attaching Observers to the Administrator Subject ----- Observer
     * Pattern---------
     */
    public void setCommanderIncomingListener(IncomingCommander ic) {
        incomingCommanderListenersList.add(ic);
    }

    public void setWorkerIncomingListener(IncomingWorker iw) {
        incomingWorkerListenersList.add(iw);
    }

    public void setCommanderOutgoingListener(OutgoingCommander oc) {
        outgoingCommanderListenersList.add(oc);
    }

    public void setWorkerOutgoingListenerr(OutgoingWorker ow) {
        outgoingWorkerListenersList.add(ow);
    }

    // Notify once the state of Administer Queue has changed to all Message
    // Observers
    public void notifyAllIncomingCommanderListeners() {
        for (MessageObserver msgObserver : incomingCommanderListenersList) {
            msgObserver.update();
        }
    }

    public void notifyAllOutgoingCommanderListeners() {
        for (MessageObserver msgObserver : outgoingCommanderListenersList) {
            msgObserver.update();
        }
    }

    public void notifyAllOutgoingWorkerListeners() {
        for (MessageObserver msgObserver : outgoingWorkerListenersList) {
            msgObserver.update();
        }
    }

    public void notifyAllIncomingWorkerListeners() {
        for (MessageObserver msgObserver : incomingWorkerListenersList) {
            msgObserver.update();
        }
    }

    public static void notifyIncomingWorkerForSteal() {
        for (MessageObserver msgObserver : incomingWorkerListenersList) {
            msgObserver.updateWorkSteal();
        }
    }


    // Incoming group-------------------------

    public void enqueueIncomingCommmand(CommandMessage message, Channel ch) {
        try {
            logger.info("An incoming command message has come. Notifying observers... ");
            CommandMessageChannelGroup entry = new CommandMessageChannelGroup(message, ch);
            incomingCommandQueue.put(entry);
            notifyAllIncomingCommanderListeners();
        } catch (InterruptedException e) {
            logger.error("message not enqueued for processing", e);
        }
    }

    public CommandMessageChannelGroup dequeueIncomingCommand() throws InterruptedException {
        logger.info("Inside dqueue incoming command ");
        return incomingCommandQueue.take();

    }

    public CommandMessageChannelGroup dequeueIncomingCommmand() throws InterruptedException {
        return incomingCommandQueue.take();
    }

    public int getIncomingCommunicationQueueSize() {
        return incomingCommandQueue.size();
    }

    public void generateIncomingCommand(CommandMessageChannelGroup msg) {
        try {
            incomingCommandQueue.putFirst(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    // Outgoing Command Group----------------

    public void enqueueOutgoingCommmand(CommandMessage message, Channel ch) {
        logger.info("Im here");
        try {
            logger.info("An outgoing command message has come. Notifying observers... ");
            CommandMessageChannelGroup entry = new CommandMessageChannelGroup(message, ch);
            outgoingCommandQueue.put(entry);
            // Notify Command Observers
            notifyAllOutgoingCommanderListeners();
        } catch (InterruptedException e) {
            logger.error("message not enqueued for processing", e);
        }
    }

    public CommandMessageChannelGroup dequeueOutgoingCommmand() throws InterruptedException {
        return outgoingCommandQueue.take();
    }

    public int getOutgoingCommunicationQueueSize() {
        return outgoingCommandQueue.size();
    }

    public int getOutgoingWorkQueueSize() {
        return outgoingWorkQueue.size();
    }

    public static LinkedBlockingDeque<WorkMessageChannelGroup> getIncomingWorkQueue() {
        return incomingWorkQueue;
    }

    public static LinkedBlockingDeque<CommandMessageChannelGroup> getIncomingCommandQueue() {
        return incomingCommandQueue;
    }

    public void generateOutgoingCommand(CommandMessageChannelGroup msg) {
        try {

            outgoingCommandQueue.putFirst(msg);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    // Incoming Work group-------------------------

    public void enqueueIncomingWork(WorkMessage message, Channel ch) {
        try {

            logger.info("An incoming work message has come. Notifying all Worker Observers... ");
            WorkMessageChannelGroup entry = new WorkMessageChannelGroup(ch, message);
            incomingWorkQueue.put(entry);

			/*---------------Notify Incoming Work Observers-------------------*/

            notifyAllIncomingWorkerListeners();

        } catch (InterruptedException e) {
            logger.error("message not enqueued for processing", e);
        }
    }

    public WorkMessageChannelGroup dequeueIncomingWork() throws InterruptedException {

        logger.info("Inside dequeue incoming work ");


        WorkMessageChannelGroup wm = incomingWorkQueue.take();

        //Now checking for work stealing
        /*if (incomingWorkQueue.isEmpty())
        {

			notifyIncomingWorkerForSteal();
		}*/

        return wm;

    }

    // Outgoing work group-------------------

    public void enqueueOutgoingWork(WorkMessage message, Channel ch) {

        try {

            logger.info("An Outgoing work message has come. Notifying all Worker Observers...");
            WorkMessageChannelGroup entry = new WorkMessageChannelGroup(ch, message);
            outgoingWorkQueue.put(entry);

			/*---------------Notify Outgoing Work Observers-------------------*/

            notifyAllOutgoingWorkerListeners();

        } catch (InterruptedException e) {
            logger.error("Message not queued yet", e);
        }
    }

    public WorkMessageChannelGroup dequeueOutgoingWork() throws InterruptedException {
        return outgoingWorkQueue.take();
    }

    public void generateIncomingWork(WorkMessageChannelGroup msg) throws InterruptedException {
        incomingWorkQueue.putFirst(msg);
    }

    public void generateOutgoingWork(WorkMessageChannelGroup msg) throws InterruptedException {
        outgoingWorkQueue.putFirst(msg);
    }


}
