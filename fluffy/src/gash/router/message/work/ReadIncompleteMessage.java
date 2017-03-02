package gash.router.message.work;

import pipe.work.Work;

public class ReadIncompleteMessage extends WorkMessage {

    public ReadIncompleteMessage(int nodeId, Work.WorkMessage message) {
        super(nodeId);
        workBuilder.setDuty(message.getDuty());
    }

    @Override
    public Work.WorkMessage getMessage() {
        workBuilder.setIsProcessed(true);
        workBuilder.setFileNotFoundInNode(true);
        return workBuilder.build();
    }
}
