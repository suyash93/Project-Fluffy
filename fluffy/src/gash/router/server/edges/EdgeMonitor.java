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
package gash.router.server.edges;

import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.MessageServer;
import gash.router.server.WorkInit;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;

/**

 * <p>
 * Edge Monitor will Monitor all edges connected to specific node. It will monitor their health and if its alive or not.
 */
public class EdgeMonitor implements EdgeListener, Runnable {
    protected static Logger logger = LoggerFactory.getLogger(EdgeMonitor.class);
    public static EdgeList aliveEdges;
    public static EdgeList edges;
    private long dt = 2000;
    private boolean forever = true;
    private MessageServer server;

    public EdgeMonitor(MessageServer server2) {
        if (server2 == null)
            throw new RuntimeException("state is null");
        this.server = server2;
        this.server.setEmon(this);

        edges = new EdgeList();
        aliveEdges = new EdgeList();

        if (server2.getConf().getRouting() != null) {
            for (RoutingEntry e : server2.getConf().getRouting()) {
                edges.addNode(e.getId(), e.getHost(), e.getPort());
            }
        }

        // cannot go below 2 sec
        if (server2.getConf().getHeartbeatDt() > this.dt)
            this.dt = server2.getConf().getHeartbeatDt();

    }


    public void createOutboundIfNew(int ref, String host, int port) {
        edges.createIfNew(ref, host, port);
    }


    public void shutdown() {
        forever = false;
    }


    private Channel connectToChannel(String host, int port) {
        Bootstrap b = new Bootstrap();
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        WorkInit workInit = new WorkInit(server, false);

        try {
            b.group(nioEventLoopGroup).channel(NioSocketChannel.class).handler(workInit);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            // Make the connection attempt.
        } catch (Exception e) {
            logger.error("Could not connect to the host " + host);
            return null;
        }
        return b.connect(host, port).syncUninterruptibly().channel();

    }

    //THIS IS NODE DISCOVERY MODULE.
    @Override
    public void run() {
        while (forever) {
            try {
                for (EdgeInfo ei : edges.map.values()) {
                    if (ei.isActive()) {
                        if (!ei.getChannel().isActive()) {
                            ei.setActive(false);
                            aliveEdges.removeNode(ei.getRef());
                        }
                    } else {
                        try {
                            logger.info("looking for edge" + ei.getRef());
                            Channel channel = connectToChannel(ei.getHost(), ei.getPort());
                            ei.setChannel(channel);
                            if (channel.isActive()) {
                                ei.setActive(true);
                                aliveEdges.addNode(ei.getRef(), ei.getHost(), ei.getPort());
                            } else {
                                if (aliveEdges.hasNode(ei.getRef())) {
                                    aliveEdges.removeNode(ei.getRef());
                                }
                            }
                        } catch (Throwable ex) {
                        }
                    }

                }
                Thread.sleep(dt);
            } catch (InterruptedException e)

            {
                e.printStackTrace();
            }
        }
    }

    public static EdgeList getEdges() {
        return edges;
    }

    public static void setEdges(EdgeList edges) {
        EdgeMonitor.edges = edges;
    }

    @Override
    public synchronized void onAdd(EdgeInfo ei) {
        server.onClusterSizeChanged(aliveEdges.size());
    }

    @Override
    public synchronized void onRemove(EdgeInfo ei) {
        server.onClusterSizeChanged(aliveEdges.size());
    }


    public void sendMessage(WorkMessage message) {
        for (EdgeInfo edgeInfo : edges.map.values()) {
            if (edgeInfo.isActive() && edgeInfo.getChannel() != null) {
                edgeInfo.getChannel().writeAndFlush(message);
            }
        }
    }

    public int getAliveNodes() {
        return aliveEdges.size();
    }
}
