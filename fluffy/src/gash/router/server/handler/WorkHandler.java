package gash.router.server.handler;

import gash.router.server.MessageServer;
import gash.router.server.chainofresponsibility.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Failure;
import pipe.work.Work.WorkMessage;


public class WorkHandler extends SimpleChannelInboundHandler<WorkMessage> {

    protected static Logger logger = LoggerFactory.getLogger(WorkHandler.class);
    protected MessageServer server;
    protected boolean debug = false;
    private Handler handler;

    public WorkHandler(MessageServer server) {
        if (server != null) {
            this.server = server;
            this.handler = new ErrorHandler(server);
            Handler hasStateHandler = new HasStateHandler(server);
            Handler pingHandler = new PingHandler(server);
            Handler heartBeatHandler = new HeartBeatHandler(server);
            Handler requestVoteHandler = new RequestVoteHandler(server);
            Handler voteHandler = new VoteHandler(server);
            Handler workStealHandler = new WorkStealHandler(server);
            Handler dutyHandler = new DutyHandler(server);

            handler.setNext(hasStateHandler);
            hasStateHandler.setNext(pingHandler);
            pingHandler.setNext(heartBeatHandler);
            heartBeatHandler.setNext(requestVoteHandler);
            requestVoteHandler.setNext(voteHandler);
            voteHandler.setNext(dutyHandler);
            dutyHandler.setNext(workStealHandler);
        }
    }

    public void handleMessage(WorkMessage msg, Channel channel) {
        if (msg == null) {
            logger.error("ERROR: Unexpected content - " + msg);
            return;
        }

        try {
            //Invoke the chain of responsibility Handlers
            handler.processWorkMessage(msg, channel);
        } catch (NullPointerException e) {
            logger.error("Null pointer has occured" + e.getMessage());
        } catch (Exception e) {
            // TODO add logging
            Failure.Builder eb = Failure.newBuilder();
            eb.setId(server.getConf().getNodeId());
            eb.setRefId(msg.getHeader().getNodeId());
            eb.setMessage(e.getMessage());
            WorkMessage.Builder rb = WorkMessage.newBuilder(msg);
            rb.setErr(eb);

            ChannelFuture cf = channel.write(rb.build());
            channel.flush();
           //cf.awaitUninterruptibly();
            if (cf.isDone() && !cf.isSuccess()) {
                logger.info("Failed to write the message to the channel ");
            }
        }

        System.out.flush();


    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WorkMessage msg) throws Exception {
        Channel channel = ctx.channel();
        handleMessage(msg, channel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }


}