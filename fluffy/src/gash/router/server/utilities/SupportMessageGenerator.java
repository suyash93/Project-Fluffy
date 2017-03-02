package gash.router.server.utilities;

import gash.router.container.GlobalConf;
import gash.router.container.RoutingConf;
import gash.router.server.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Header;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkMessage.StateOfLeader;
import pipe.work.Work.WorkStealing;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class SupportMessageGenerator {

    private static RoutingConf conf;
    private static final Logger logger = LoggerFactory.getLogger(SupportMessageGenerator.class);

    protected static AtomicReference<SupportMessageGenerator> instance = new AtomicReference<SupportMessageGenerator>();

    public static SupportMessageGenerator initSupportGenerator() {
        instance.compareAndSet(null, new SupportMessageGenerator());
        return instance.get();
    }

    private SupportMessageGenerator() {

    }

    public static SupportMessageGenerator getMessageInstance() {
        SupportMessageGenerator supportMsg = instance.get();
        if (supportMsg == null) {
            logger.error(" Error while getting instance of SupportMessageGenerator ");
        }
        return supportMsg;
    }


    public WorkMessage produceWorkStealingMessage() {

        logger.info("Inside work stealing message");
        Header.Builder hb = Header.newBuilder();
        //TODO Get node ID
        hb.setNodeId(conf.getNodeId());
        hb.setTime(System.currentTimeMillis());

        WorkStealing.Builder stealMessage = WorkStealing.newBuilder();
        stealMessage.setStealtype(pipe.work.Work.WorkStealing.StealType.STEAL_REQUEST);

        WorkMessage.Builder wb = WorkMessage.newBuilder();
        wb.setHeader(hb.build());
        wb.setIsProcessed(false);
        //TODO Set the secret
        wb.setSecret(1234);
        addLeaderFieldToWorkMessage(wb);
        wb.setSteal(stealMessage);

        return wb.build();
    }


    private void addLeaderFieldToWorkMessage(WorkMessage.Builder wb) {
        if (NodeManager.currentLeaderid == 0) {
            wb.setStateOfLeader(StateOfLeader.LEADERNOTKNOWN);
        } else if (NodeManager.currentLeaderid == conf.getNodeId()) {
            // Current Node is the leader
            wb.setStateOfLeader(StateOfLeader.LEADERALIVE);
        } else {
            wb.setStateOfLeader(StateOfLeader.LEADERKNOWN);
        }
    }

    public static void setRoutingConf(RoutingConf routingConf) {
        SupportMessageGenerator.conf = routingConf;
    }

    public static String generateRequestID() {
        UUID uuid = UUID.randomUUID();
        String uidString = uuid.toString();
        return uidString;
    }


}
