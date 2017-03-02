package gash.router.message.work;

import pipe.work.Work;


public class WriteCompleteMessage extends WorkMessage {

    public WriteCompleteMessage(int nodeId, Work.WorkMessage workMessage) {
        super(nodeId);
        workBuilder.setDuty(workMessage.getDuty());
        workBuilder.setIsProcessed(true);
    }

    @Override
    public Work.WorkMessage getMessage() {
        return workBuilder.build();
    }
}
