package gash.router.server;

import gash.router.server.model.CommandMessageChannelGroup;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Duty;
import routing.Pipe.CommandMessage;

/**
 * Incoming Commander
 *

 */

public class IncomingCommander extends MessageObserver {

    private static final Logger logger = LoggerFactory.getLogger(IncomingCommander.class);

    private AdministerQueue administratorSubject;
    protected boolean isSuccess;


    public IncomingCommander(MessageServer server) {
        super();
        administratorSubject = AdministerQueue.getInstance();
        administratorSubject.setCommanderIncomingListener(this);
    }

    @Override
    public void update() {
        try {
            logger.info("Inside update of Incoming Commander");
            CommandMessageChannelGroup currentCommandMessageGroup = administratorSubject.dequeueIncomingCommmand();
            Channel currentChannel = currentCommandMessageGroup.getChannel();
            CommandMessage currMsg = currentCommandMessageGroup.getCommandMessage();
            Duty currentDuty = currentCommandMessageGroup.getCommandMessage().getDuty();
            logger.info("Printing filename " + currMsg.getDuty().getFilename());
            String requestId = currentDuty.getRequestId();
            // Store the client request id along with its channel to respond back later

            //logger.info("Now doing replication");

            /**TODO left with storing request id **/

            //server.getNodeManager().addClientChannelwithRequestId(currentCommandMessageGroup, requestId);
            //server.getDataReplicationManager().replicate(currMsg);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    @Override
    public void updateWorkSteal() {
        // TODO Auto-generated method stub

    }

}
