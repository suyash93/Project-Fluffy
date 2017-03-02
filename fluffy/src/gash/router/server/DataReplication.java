package gash.router.server;

import gash.router.client.CommConnection;
import io.netty.channel.Channel;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;

public class DataReplication {

    protected static Logger logger = LoggerFactory.getLogger("edge monitor");

    protected MessageServerImpl server;
    protected CommConnection commConn;
    protected Channel channel;
    public EdgeList edges;

    public DataReplication(MessageServerImpl server) {
        this.server = server;
    }

    public void replicateData(WorkMessage msg) {

        //EdgeMonitor edgeMonitor = new EdgeMonitor(server);

        edges = EdgeMonitor.getEdges();

        for (EdgeInfo ei : this.edges.map.values()) {
            System.out.println("Inside for loop of edge info " + ei);

            try {

                commConn = CommConnection.initConnection(ei.getHost(), ei.getPort());
                channel = commConn.connect();
                ei.setActive(true);
                ei.setChannel(channel);

                if (ei.isActive() && ei.getChannel() != null) {

                    // WorkMessage wm = createHB(ei);

                    WorkMessage.Builder wb = WorkMessage.newBuilder();
                    wb.setClientMessage(msg.getClientMessage());
                    ei.getChannel().writeAndFlush(wb.build());

                } else {

                    logger.info("Channel not active");
                }

            } catch (Exception ex) {
                logger.info("trying to connect to node " + ei.getRef() + ei.getChannel());
            }

        }
    }
}
