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
import gash.router.server.chainofresponsibility.Handler;
import gash.router.server.chainofresponsibility.PingHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Failure;
import routing.Pipe.CommandMessage;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * <p>
 *
 * @author gash
 */
public class CommandHandler extends SimpleChannelInboundHandler<CommandMessage> {
    protected static Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    protected MessageServer server;
    private Handler handler;

    public CommandHandler(MessageServer server) {
        if (server != null) {
            this.server = server;
            handler = new PingHandler(server);
            DutyHandler dutyHandler = new DutyHandler(server);
            handler.setNext(dutyHandler);
        }
    }

    /**
     * override this method to provide processing behavior. This implementation
     * mimics the routing we see in annotating classes to support a RESTful-like
     * behavior (e.g., jax-rs).
     *
     * @param msg
     */
    public void handleMessage(CommandMessage msg, Channel channel) {
    	//logger.info("Got message from server or client is sending?");
        if (msg == null) {
            logger.error("Unexpected content - " + msg);
            return;
        }
        try {
            handler.processCommandMessage(msg, channel);
        } catch (Exception e) {
            logger.error("Failed to process message " + msg.toString());
            Failure.Builder eb = Failure.newBuilder();
            eb.setId(server.getConf().getNodeId());
            eb.setRefId(msg.getHeader().getNodeId());
            eb.setMessage(e.getMessage());
            CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
            rb.setErr(eb);
            channel.write(rb.build());
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
    protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
        handleMessage(msg, ctx.channel());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }


}