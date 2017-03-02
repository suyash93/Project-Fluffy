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
package gash.router.global.edges;

import gash.router.container.GlobalConf;
import gash.router.server.GlobalInit;
import gash.router.server.MessageServer;
import global.Global;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 
 * <p>
 * Edge Monitor will Monitor all edges connected to specific node. It will monitor their health and if its alive or not.
 */
public class GlobalEdgeMonitor implements GlobalEdgeListener, Runnable {
    protected static Logger logger = LoggerFactory.getLogger(GlobalEdgeMonitor.class);
    public static GlobalEdgeList aliveEdges;
    public static GlobalEdgeList edges;
    private long dt = 2000;
    private boolean forever = true;
    private MessageServer server;

    public GlobalEdgeMonitor(MessageServer server) {
        if (server == null)
            throw new RuntimeException("state is null");
        this.server = server;
        edges = new GlobalEdgeList();
        aliveEdges = new GlobalEdgeList();

        if (server.getGlobalConf().getRouting() != null) {
            for (GlobalConf.GlobalRoutingEntry e : server.getGlobalConf().getRouting()) {
                edges.addNode(e.getClusterId(), e.getHost(), e.getPort());
            }
        }

        // cannot go below 2 sec
        if (server.getConf().getHeartbeatDt() > this.dt)
            this.dt = server.getConf().getHeartbeatDt();

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
        GlobalInit globalInit = new GlobalInit(server, false);

        try {
            b.group(nioEventLoopGroup).channel(NioSocketChannel.class).handler(globalInit);
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
                for (GlobalEdgeInfo ei : edges.map.values()) {
                    if (ei.isActive()) {
                        if (!ei.getChannel().isActive()) {
                            ei.setActive(false);
                            aliveEdges.removeNode(ei.getHost());
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
                                if (aliveEdges.hasNode(ei.getHost())) {
                                    aliveEdges.removeNode(ei.getHost());
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

    public static GlobalEdgeList getEdges() {
        return edges;
    }

    public static void setEdges(GlobalEdgeList edges) {
        GlobalEdgeMonitor.edges = edges;
    }


    public  static void sendMessage(Global.GlobalMessage message) {
        for (GlobalEdgeInfo edgeInfo : edges.map.values()) {
            if (edgeInfo.isActive() && edgeInfo.getChannel() != null) {
                edgeInfo.getChannel().writeAndFlush(message);
            }
        }
    }

    public int getAliveNodes() {
        return aliveEdges.size();
    }

    @Override
    public void onAdd(GlobalEdgeInfo ei) {
        server.onClusterSizeChanged(aliveEdges.size());
    }

    @Override
    public void onRemove(GlobalEdgeInfo ei) {
        server.onClusterSizeChanged(aliveEdges.size());
    }
}
