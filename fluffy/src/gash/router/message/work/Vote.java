package gash.router.message.work;

import pipe.leaderelection.Leaderelection;
import pipe.work.Work;


public class Vote extends WorkMessage {
    private Leaderelection.Vote.Builder voteBuilder;

    public Vote(int nodeId, int currentTerm, int candidateId) {
        super(nodeId);
        voteBuilder = Leaderelection.Vote.newBuilder();
        voteBuilder.setCurrentTerm(currentTerm);
        voteBuilder.setVoterID(nodeId);
    }

    @Override
    public Work.WorkMessage getMessage() {
        workBuilder.setVote(voteBuilder);
        return workBuilder.build();
    }



}
