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
package gash.router.server.handler;


import gash.router.server.MessageServer;
import gash.router.server.chainofresponsibility.DutyHandler;
import gash.router.server.chainofresponsibility.GlobalResponseHandler;
import gash.router.server.chainofresponsibility.Handler;
import gash.router.server.chainofresponsibility.PingHandler;
import global.Global.GlobalMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * <p>
 * TODO replace println with logging!
 *
 * @author gash
 */
public class GlobalHandler extends SimpleChannelInboundHandler<GlobalMessage> {
    protected static Logger logger = LoggerFactory.getLogger(GlobalHandler.class);
    protected MessageServer server;
    protected boolean debug = false;
    private Handler handler;

    public GlobalHandler(MessageServer server) {
        if (server != null) {
            this.server = server;
            handler = new PingHandler(server);
            DutyHandler dutyHandler = new DutyHandler(server);
            GlobalResponseHandler globalResponseHandler = new GlobalResponseHandler(server);
            handler.setNext(dutyHandler);
           dutyHandler.setNext(globalResponseHandler);
        }
    }

    /**
     * override this method to provide processing behavior. T
     *
     * @param msg
     */
    public void handleMessage(GlobalMessage msg, Channel channel) {
        logger.info("Messaage Coming "+ msg.toString());
        if (msg == null) {
            // TODO add logging
            System.out.println("ERROR: Unexpected content - " + msg);
            return;
        }

        try {
            handler.processGlobalMessage(msg, channel);
            if (msg.hasResponse()) {
                logger.info(msg.toString());
            }
        } catch (Exception e) {
            // TODO add logging
            System.out.println("Caught Exception in Global Handler!!!!!!!!!!!!!!!!!!!!!!");
        }

        System.out.flush();

    }

    /**
     * a message was received from the server. Here we dispatch the message to
     * the client's thread pool to minimize the time it takes to process other
     * messages.
     *
     * @param ctx The channel the message was received from
     * @param msg The message
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GlobalMessage msg) throws Exception {
        handleMessage(msg, ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }

}