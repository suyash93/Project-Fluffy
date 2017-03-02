/**
 * Copyright 2016 Gash.
 * <p>
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server;

import gash.router.container.GlobalConf;
import gash.router.container.RoutingConf;
import gash.router.inmemory.RiakClientHandler;
import gash.router.persistence.replication.DataReplicationManager;
import gash.router.persistence.replication.HashFunction;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.state.FollowerState;
import gash.router.server.state.ServerState;
import gash.router.server.tasks.NoOpBalancer;
import gash.router.server.tasks.TaskList;
import gash.router.server.utilities.SupportMessageGenerator;
import global.Global;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work;
import routing.Pipe;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class MessageServerImpl implements MessageServer {
    protected static Logger logger = LoggerFactory.getLogger("server");
    private static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();
    private AdministerQueue administerQueue;
    protected RoutingConf conf;
    private GlobalConf globalConf;
    protected boolean background = false;
    public static int processed = 0;
    public static int stolen = 0;
    public static int nodeId;
    private ServerState currentState;
    private int currentTerm = 0;
    private int currentLeader = 0;
    private EdgeMonitor edgeMonitor;
    private NodeManager nodeManager;
    private DataReplicationManager dataReplicationMgr;
    private TaskList tasks;
    public static boolean monitoringStatus = false;
    public static Channel globalChannel;

    private IncomingCommander incomingCommander;
    private OutgoingCommander outgoingCommander;
    private IncomingWorker incomingWorker;
    private OutgoingWorker outgoingWorker;

    /**
     * initialize the server with a configuration of it's resources
     *
     * @param cfg
     */
    public MessageServerImpl(File cfg, File commonClusterRoutingConfig, boolean isMonitering) {
        init(cfg);
        initCommonClusterRoutingConfig(commonClusterRoutingConfig);
        monitoringStatus = isMonitering;
        setMonitoringStatus(monitoringStatus);
        globalChannel = null;
    }

    public MessageServerImpl(RoutingConf conf, GlobalConf globalConf) {
        this.conf = conf;
        this.globalConf = globalConf;
    }

    public void release() {
    }

    public void startServer() {
        // Instantiating Node Manager
        // nodeManager=NodeManager.initNodeManager(MessageServerImpl.this);

		/* Initiliazing all queue commanders and workers ****/

        administerQueue = AdministerQueue.initAdministration();
        incomingCommander = new IncomingCommander(MessageServerImpl.this);
        outgoingCommander = new OutgoingCommander(MessageServerImpl.this);
        incomingWorker = new IncomingWorker(MessageServerImpl.this);
        Thread t = new Thread(incomingWorker);
        t.start();
        outgoingWorker = new OutgoingWorker(MessageServerImpl.this);

        try {
            dataReplicationMgr = DataReplicationManager.initDataReplicationManager(new HashFunction(), MessageServerImpl.this);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        RiakClientHandler.init();
        SupportMessageGenerator.initSupportGenerator();
        SupportMessageGenerator.setRoutingConf(conf);


        /** Instantiating Worker Communication **/

        StartWorkCommunication comm = new StartWorkCommunication(conf);
        logger.info("Work starting");

        // We always start the worker in the background
        Thread cthread = new Thread(comm);
        cthread.start();

        StartGlobalCommunication globalComm = new StartGlobalCommunication(globalConf);
        logger.info("Global staring");
        Thread gThread = new Thread(globalComm);
        gThread.start();


        if (!conf.isInternalNode()) {
            /** Instantiate Command Communication **/
            StartCommandCommunication comm2 = new StartCommandCommunication(conf);
            logger.info("Command starting");

            if (background) {
                Thread cthread2 = new Thread(comm2);
                cthread2.start();
            } else
                comm2.run();
        }


    }

    /**
     * static because we need to get a handle to the factory from the shutdown
     * resource
     */
    public static void shutdown() {
        logger.info("Server shutdown");
        System.exit(0);
    }

    private void init(File cfg) {
        if (!cfg.exists())
            throw new RuntimeException(cfg.getAbsolutePath() + " not found");
        // resource initialization - how message are processed
        BufferedInputStream br = null;
        try {
            byte[] raw = new byte[(int) cfg.length()];
            br = new BufferedInputStream(new FileInputStream(cfg));
            br.read(raw);
            conf = JsonUtil.decode(new String(raw), RoutingConf.class);
            if (!verifyConf(conf))
                throw new RuntimeException("verification of configuration failed");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initCommonClusterRoutingConfig(File commonClusterRoutingConfig) {
        if (!commonClusterRoutingConfig.exists())
            throw new RuntimeException(commonClusterRoutingConfig.getAbsolutePath() + " not found");
        // resource initialization - how message are processed
        BufferedInputStream br = null;
        try {
            byte[] raw = new byte[(int) commonClusterRoutingConfig.length()];
            br = new BufferedInputStream(new FileInputStream(commonClusterRoutingConfig));
            br.read(raw);
            this.globalConf = JsonUtil.decode(new String(raw), GlobalConf.class);
            if (!verifyCommonClusterConf(this.globalConf))
                throw new RuntimeException("verification of common cluster configuration failed");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean verifyConf(RoutingConf conf) {
        return (conf != null);
    }

    private boolean verifyCommonClusterConf(GlobalConf conf) {
        return (conf != null);
    }

    /**
     * initialize command communication
     */
    private class StartCommandCommunication implements Runnable {

        RoutingConf conf;

        public StartCommandCommunication(RoutingConf conf) {
            this.conf = conf;
        }

        public void run() {
            // construct boss and worker threads (num threads = number of cores)

            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap b = new ServerBootstrap();
                bootstrap.put(conf.getCommandPort(), b);

                b.group(bossGroup, workerGroup);
                b.channel(NioServerSocketChannel.class);
                b.option(ChannelOption.SO_BACKLOG, 100);
                b.option(ChannelOption.TCP_NODELAY, true);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                // b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

                boolean compressComm = false;
                b.childHandler(new CommandInit(MessageServerImpl.this, compressComm));

                // Start the server.
                logger.info("Starting command server (" + conf.getNodeId() + "), listening on port = "
                        + conf.getCommandPort());
                ChannelFuture f = b.bind(conf.getCommandPort()).syncUninterruptibly();

                logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
                        + f.channel().isWritable() + ", act: " + f.channel().isActive());

                // block until the server socket is closed.
                f.channel().closeFuture().sync();

            } catch (Exception ex) {
                // on bind().sync()
                logger.error("Failed to setup handler.", ex);
            } finally {
                // Shut down all event loops to terminate all threads.
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }

    /**
     * initialize work communication
     */
    private class StartWorkCommunication implements Runnable {

        public StartWorkCommunication(RoutingConf conf) {
            System.out.println("Inside Start work communication");
            if (conf == null)
                throw new RuntimeException("missing conf");
            TaskList tasks = new TaskList(new NoOpBalancer());
            currentState = new FollowerState(MessageServerImpl.this);
            EdgeMonitor emon = new EdgeMonitor(MessageServerImpl.this);

            Thread t = new Thread(emon);
            System.out.println("Starting edge monitor thread");
            t.start();


            nodeManager = NodeManager.initNodeManager(MessageServerImpl.this);
        }

        public void run() {
            // construct boss and worker threads (num threads = number of cores)

            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap b = new ServerBootstrap();
                bootstrap.put(conf.getWorkPort(), b);

                b.group(bossGroup, workerGroup);
                b.channel(NioServerSocketChannel.class);
                b.option(ChannelOption.SO_BACKLOG, 100);
                b.option(ChannelOption.TCP_NODELAY, true);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                // b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

                boolean compressComm = false;
                b.childHandler(new WorkInit(MessageServerImpl.this, compressComm));

                // Start the server.
                logger.info(
                        "Starting work server (" + conf.getNodeId() + "), listening on port = " + conf.getWorkPort());
                ChannelFuture f = b.bind(conf.getWorkPort()).syncUninterruptibly();

                logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
                        + f.channel().isWritable() + ", act: " + f.channel().isActive());

                // block until the server socket is closed.
                f.channel().closeFuture().sync();

            } catch (Exception ex) {
                // on bind().sync()
                logger.error("Failed to setup handler.", ex);
            } finally {
                // Shut down all event loops to terminate all threads.
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();

                // shutdown monitor

                if (edgeMonitor != null)
                    edgeMonitor.shutdown();
            }
        }
    }

    /**
     * Initialize Global Communication
     */
    private class StartGlobalCommunication implements Runnable {
        GlobalConf globalConf;


        public StartGlobalCommunication(GlobalConf globalConf) {
            if (globalConf == null)
                throw new RuntimeException("missing CommonCluster conf");
            this.globalConf = globalConf;
        }

        public void run() {
            // construct boss and worker threads (num threads = number of cores)
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap b = new ServerBootstrap();
                logger.info("Global Command Host & Port ::: " + globalConf.getGlobalHost()
                        + globalConf.getGlobalPort());
                bootstrap.put(globalConf.getGlobalPort(), b);

                b.group(bossGroup, workerGroup);
                b.channel(NioServerSocketChannel.class);
                b.option(ChannelOption.SO_BACKLOG, 100);
                b.option(ChannelOption.TCP_NODELAY, true);
                b.option(ChannelOption.SO_KEEPALIVE, true);

                boolean compressComm = false;
                b.childHandler(new GlobalInit(MessageServerImpl.this, compressComm));

                // Start the server.
                logger.info("Starting Common Cluster Command server (" + globalConf.getClusterId()
                        + "), listening on port = " + globalConf.getGlobalPort());
                ChannelFuture f = b.bind(globalConf.getGlobalPort()).syncUninterruptibly();
                logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
                        + f.channel().isWritable() + ", act: " + f.channel().isActive());

                // block until the server socket is closed.
                f.channel().closeFuture().sync();

            } catch (Exception ex) {
                // on bind().sync()
                logger.error("Failed to setup handler.", ex);
            } finally {
                // Shut down all event loops to terminate all threads.
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }

    /**
     * help with processing the configuration information
     *
     * @author gash
     */
    public static class JsonUtil {
        private static JsonUtil instance;

        public static void init(File cfg) {

        }

        public static JsonUtil getInstance() {
            if (instance == null)
                throw new RuntimeException("Server has not been initialized");

            return instance;
        }

        public static String encode(Object data) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(data);
            } catch (Exception ex) {
                return null;
            }
        }

        public static <T> T decode(String data, Class<T> theClass) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(data.getBytes(), theClass);
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public static int getNodeId() {
        return nodeId;
    }

    public static boolean getMonitoringStatus() {
        if (monitoringStatus) {
            return true;
        }
        return false;
    }

    public static void setMonitoringStatus(boolean status) {
        monitoringStatus = status;
    }

    public GlobalConf getGlobalConf() {
        return globalConf;
    }

    public HashMap<Integer, ServerBootstrap> getBootstrap() {
        return bootstrap;
    }

    public EdgeMonitor getEmon() {
        return edgeMonitor;
    }

    public void setEmon(EdgeMonitor emon) {
        this.edgeMonitor = emon;
    }

    public EdgeMonitor getGmon() {
        return edgeMonitor;
    }

    public TaskList getTasks() {
        return tasks;
    }

    public void setTasks(TaskList tasks) {
        this.tasks = tasks;
    }

    public void setCurrentState(ServerState serverState) {
        this.currentState = serverState;
    }

    public ServerState getCurrentState() {
        return currentState;
    }

    @Override
    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public DataReplicationManager getDataReplicationManager() {
        return dataReplicationMgr;
    }

    public void setNodeManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    /**
     * vote to candidate.
     *
     * @param workMessage Work Message
     * @param channel     channel
     */
    @Override
    public void vote(Work.WorkMessage workMessage, Channel channel) {
        currentState.vote(workMessage, channel);
    }

    /**
     * ask for vote.
     */
    @Override
    public void askForVote(int currentTerm, int candidateId) {
        currentState.startElection(currentTerm, candidateId);
    }

    public RoutingConf getConf() {
        return conf;
    }

    public void setConf(RoutingConf conf) {
        this.conf = conf;
    }

    @Override
    public void incrementCurrentTerm() {
        currentTerm++;
    }

    public void setCurrentLeader(int currentLeader) {
        this.currentLeader = currentLeader;
        NodeManager.currentLeaderid = currentLeader;
    }

    @Override
    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    @Override
    public int getCurrentLeader() {
        return currentLeader;
    }

    @Override
    public int getCurrentTerm() {
        return currentTerm;
    }

    public EdgeMonitor getEdgeMonitor() {
        return edgeMonitor;
    }


    @Override
    public void onHeartBeat(Work.WorkMessage workMessage, Channel channel) {
        currentState.onHeartBeat(workMessage, channel);
    }

    @Override
    public void onVoteReceived(Work.WorkMessage workMessage, Channel channel) {
        currentState.onVoteReceived(workMessage, channel);
    }

    @Override
    public void onClusterSizeChanged(int sizeofCluster) {
        currentState.onClusterSizeChanged(sizeofCluster);
    }

    @Override
    public void onDutyMessage(Pipe.CommandMessage message, Channel channel) {
        currentState.onDutyReceived(message, channel);
    }

    @Override
    public void onGlobalDutyMessage(Global.GlobalMessage message, Channel channel) {
        currentState.onGlobalDutyReceived(message, channel);
    }

    @Override
    public void onInMemoryReadRequest(Work.WorkMessage message, Channel channel) {
        currentState.onInternalReadRequest(message, channel);
    }

    @Override
    public void onExternalReadRequest(Work.WorkMessage message, Channel channel) {
        currentState.onExternalReadRequest(message, channel);
    }

    @Override
    public void onInMemoryWriteRequest(Work.WorkMessage message, Channel channel) {
        currentState.onInternalWriteRequest(message, channel);
    }

    @Override
    public void onExternalWriteRequest(Work.WorkMessage message, Channel channel) {
        currentState.onExternalWriteRequest(message, channel);
    }

    @Override
    public void onInMemoryUpdateRequest(Work.WorkMessage message, Channel channel) {
        currentState.onInternalUpdateRequest(message, channel);
    }

    @Override
    public void onInMemoryDeleteRequest(Work.WorkMessage message, Channel channel) {
        currentState.onInternalDeleteRequest(message, channel);
    }

    @Override
    public void onExternalUpdateRequest(Work.WorkMessage message, Channel channel) {

        currentState.onExternalUpdateRequest(message, channel);
    }

    @Override
    public void onExternalDeleteRequest(Work.WorkMessage message, Channel channel) {
        currentState.onExternalDeleteRequest(message, channel);
    }

    @Override
    public void onReadComplete(Work.WorkMessage message, Channel channel) {
        currentState.onReadComplete(message, channel);
    }

    @Override
    public void onWriteComplete(Work.WorkMessage message, Channel channel) {
        currentState.onWriteComplete(message, channel);
    }

    @Override
    public void onFileNotFound(Work.WorkMessage message, Channel channel) {
        currentState.onFileNotFound(message, channel);
    }

    @Override
    public void onGlobalResponseRecevied(Global.GlobalMessage message, Channel channel) {
        currentState.onGlobalResponseReceived(message, channel);
    }

}
