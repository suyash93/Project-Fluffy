package gash.router.persistence.replication;

import gash.router.server.AdministerQueue;
import gash.router.server.MessageServerImpl;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.utilities.SupportMessageGenerator;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

public class DataReplicationManager {

    private final HashFunction hashFunction;
    private SortedMap<BigInteger, EdgeInfo> circle = new TreeMap<BigInteger, EdgeInfo>();

    protected static Logger logger = LoggerFactory.getLogger("DataReplicationManager");
    protected static AtomicReference<DataReplicationManager> instance = new AtomicReference<DataReplicationManager>();
    protected MessageServerImpl server;
    protected Channel channel;
    public EdgeList edges;

    public static DataReplicationManager initDataReplicationManager(HashFunction hashFunction, MessageServerImpl server) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        instance.compareAndSet(null, new DataReplicationManager(hashFunction, server));
        System.out.println(" --- Initializing Data Replication Manager --- ");
        return instance.get();
    }

    public static DataReplicationManager getInstance() throws Exception {
        if (instance != null && instance.get() != null) {
            return instance.get();
        }
        throw new Exception("Data Replication Manager not started ");
    }

    public DataReplicationManager(HashFunction hashFunction, MessageServerImpl server)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        this.hashFunction = hashFunction;

        this.server = server;

		/*
         * for (EdgeInfo node : nodes) { add(node); }
		 */
    }

    public void add(EdgeInfo node) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        logger.info("Adding node to circle " + node.getHost());
        circle.put(hashFunction.md5hash(node.toString()), node);

    }

    public void remove(EdgeInfo node) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        circle.remove(hashFunction.md5hash(node.toString()));

    }

    public EdgeInfo get(Object key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        logger.info("Inside get of consistent hash");
        if (circle.isEmpty()) {
            return null;
        }
        BigInteger hash = hashFunction.md5hash(key.toString());
        if (!circle.containsKey(hash)) {
            SortedMap<BigInteger, EdgeInfo> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public void replicate(WorkMessage message) {

        /****
         * Implemented using consistent hashing
         */

        logger.info("inside replicate of consistent hash");
        edges = EdgeMonitor.edges;

        try {
            for (EdgeInfo ei : this.edges.map.values()) {
                if (ei.isActive() && ei.getChannel() != null) {

                    add(ei);

                }

            }

            EdgeInfo einfo = get(message.getRequestId());
            logger.info("Printing edge where the data needs to be replicated  " + einfo.getRef());

            Channel nodeChannel = einfo.getChannel();
            int nodeId = einfo.getRef();
            logger.info("Inside replicate now printing nodeChannel" + nodeChannel);
            Replication replication = new Replication(message, nodeChannel, nodeId);
            Thread replicationThread = new Thread(replication);
            replicationThread.start();

        } catch (Exception e) {

        }
    }

    // Implemented using Future and Callable

    private class Replication implements Runnable {
        private WorkMessage workMessage;
        private Channel nodeChannel;
        private int nodeId;

        public Replication(WorkMessage workMessage, Channel nodeChannel, Integer nodeId) {
            // Command Message here contains the chunk ID and the chunk nos. and
            // the chunk byte, in its Task field
            this.workMessage = workMessage;
            this.nodeChannel = nodeChannel;
            this.nodeId = nodeId;

            logger.info("Printing  node Channel after invoking constructor");
        }

        @Override
        public void run() {
            logger.info("Printing node channel" + nodeChannel);
            if (this.nodeChannel.isOpen() && this.nodeChannel.isActive()) {
                // Push this message to the outbound work queue
                try {
                    logger.info("Sending message for replication ");
                    AdministerQueue.getInstance().enqueueOutgoingWork(workMessage, nodeChannel);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    e.printStackTrace();
                }
            } else {
                logger.error("The nodeChannel to " + nodeChannel.localAddress() + " is not active");
            }
        }

    }

}