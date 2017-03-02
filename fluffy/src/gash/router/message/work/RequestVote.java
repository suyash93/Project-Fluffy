package gash.router.message.work;

import pipe.leaderelection.Leaderelection;
import pipe.work.Work;


public class RequestVote extends WorkMessage {
    private Leaderelection.RequestVote.Builder requestVoteBuilder;

    public RequestVote(int nodeId, int currentTerm, int candidateId) {
        super(nodeId);
        requestVoteBuilder = Leaderelection.RequestVote.newBuilder();
        requestVoteBuilder.setCandidateID(candidateId);
        requestVoteBuilder.setCurrentTerm(currentTerm);
    }

    @Override
    public Work.WorkMessage getMessage() {
        workBuilder.setRequestVote(requestVoteBuilder);
        return workBuilder.build();
    }


}
