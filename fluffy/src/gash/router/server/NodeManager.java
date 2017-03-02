package gash.router.server;

import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.model.WorkMessageChannelGroup;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class NodeManager {

    protected static Logger logger = LoggerFactory.getLogger(NodeManager.class);

    protected static AtomicReference<NodeManager> instance = new AtomicReference<NodeManager>();

    public static ConcurrentHashMap<Integer, Channel> nodeChannelMap = new ConcurrentHashMap<Integer, Channel>();

    public static ConcurrentHashMap<String, WorkMessageChannelGroup> clientChannelMap = new ConcurrentHashMap<String, WorkMessageChannelGroup>();

    private static Queue<Integer> roundRobinQueue = new LinkedBlockingQueue<Integer>();
    private static Queue<Integer> workStealQueue = new LinkedBlockingQueue<Integer>();
    private static int nodeId = 0;

    public static int currentLeaderid;
    public static boolean doIExistInNetwork = true;
    public static String currentLeaderAddress;

    private boolean forever = true;

    private static Channel commonClusterAdapterChannel;
    private static MessageServerImpl server;

    private static ConcurrentHashMap<Integer, Channel> commonClusterChannel = new ConcurrentHashMap<Integer, Channel>();

    public NodeManager(MessageServerImpl serverImpl) {
        server = serverImpl;
        server.setNodeManager(this);
        NodeMonitor nodeMonitor = new NodeMonitor();
        Thread t = new Thread(nodeMonitor);

        t.start();

    }

    public static NodeManager initNodeManager(MessageServerImpl server) {
        instance.compareAndSet(null, new NodeManager(server));
        System.out.println("-----Initializing Node Manager-----");
        return instance.get();

    }

    private class NodeMonitor implements Runnable {

        @Override

        public void run() {

            while (forever) {

                EdgeList aliveEdges = EdgeMonitor.edges;

                for (EdgeInfo edgeInfo : aliveEdges.map.values()) {
                    if (edgeInfo.isActive() && edgeInfo.getChannel() != null) {

                        if (!roundRobinQueue.contains(edgeInfo.getRef())) {
                            roundRobinQueue.add(edgeInfo.getRef());
                        }
                        if (!workStealQueue.contains(edgeInfo.getRef())) {
                            workStealQueue.add(edgeInfo.getRef());
                        }

                    }
                }
            }

        }

    }

    public void addClientChannelwithRequestId(WorkMessageChannelGroup clientChannel, String requestId) {
        clientChannelMap.put(requestId, clientChannel);
    }

    /**
     * Retrieves the client's channel from the stored map
     */
    public synchronized WorkMessageChannelGroup getClientChannelFromMap(String requestId) {

        if (clientChannelMap.containsKey(requestId) && clientChannelMap.get(requestId) != null) {
            return clientChannelMap.get(requestId);
        }
        logger.info("Unable to find the channel for request ID : " + requestId);
        return null;
    }

    public synchronized void removeClientChannelFromMap(String requestId) throws Exception {
        if (clientChannelMap.containsKey(requestId) && clientChannelMap.get(requestId) != null) {
            clientChannelMap.remove(requestId);
        } else {
            logger.error("Unable to find the channel for request ID : " + requestId);
            throw new Exception("Unable to find the node for this request ID : " + requestId);
        }

    }

    /**
     * Obsolete since the edge monitor has all channel info of all edges
     **/

	/*
     * public static Channel getChannelByNodeId(int nodeId) { return
	 * nodeChannelMap.get(nodeId); }
	 */
    public ConcurrentHashMap<String, WorkMessageChannelGroup> getClientChannelMap() {
        return clientChannelMap;
    }

    public static Channel getNextReadChannel() {
        if (!roundRobinQueue.isEmpty()) {
            Integer nodeId = roundRobinQueue.remove();
            if (nodeId == server.getCurrentLeader()) {
                roundRobinQueue.add(nodeId);
                nodeId = roundRobinQueue.remove();
            }
            if (EdgeMonitor.edges.hasNode(nodeId)) {
                roundRobinQueue.add(nodeId);
                //return nodeChannelMap.get(nodeId);
                return EdgeMonitor.edges.getNode(nodeId).getChannel();

            }
            roundRobinQueue.add(nodeId);
        }
        logger.info("No channel found ");
        return null;
    }

    public static Channel getNextChannelForWorkStealing() {

        logger.info("Inside Work stealing method");
        if (!workStealQueue.isEmpty()) {

            Integer nodeId = workStealQueue.remove();

            //if (nodeId == server.getCurrentLeader()) {
                //logger.info("My Neighbour is leader......................");
                //workStealQueue.add(nodeId);
                //nodeId = workStealQueue.remove();
            //}
            if (EdgeMonitor.edges.hasNode(nodeId)) {
                workStealQueue.add(nodeId);
                //return nodeChannelMap.get(nodeId);
                return EdgeMonitor.edges.getNode(nodeId).getChannel();

            }
            workStealQueue.add(nodeId);
        } else {
            logger.info("No channel found ");
        }

        return null;
    }

}
