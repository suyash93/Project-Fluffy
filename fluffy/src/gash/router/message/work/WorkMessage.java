package gash.router.message.work;


import pipe.common.Common;
import pipe.work.Work;

public abstract class WorkMessage {
    protected Work.WorkMessage.Builder workBuilder;

    public WorkMessage(int nodeId) {
        Common.Header.Builder headerBuilder = Common.Header.newBuilder();
        headerBuilder.setNodeId(nodeId);
        headerBuilder.setDestination(-1);
        headerBuilder.setTime(System.currentTimeMillis());
        workBuilder = Work.WorkMessage.newBuilder();
        workBuilder.setHeader(headerBuilder);
        workBuilder.setSecret(1234);
    }

    public abstract Work.WorkMessage getMessage();




}
