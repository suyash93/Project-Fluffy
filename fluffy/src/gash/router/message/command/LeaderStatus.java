package gash.router.message.command;


import pipe.leaderelection.Leaderelection;
import routing.Pipe;


public class LeaderStatus extends CommandMessage {
    private Leaderelection.StatusOfLeader.Builder leaderStatus;

    public LeaderStatus(int nodeId, Leaderelection.StatusOfLeader.LeaderState leaderState, Leaderelection.StatusOfLeader.AskLeader action, int leaderId) {
        super(nodeId);
        leaderStatus = Leaderelection.StatusOfLeader.newBuilder();
        leaderStatus.setLeaderState(leaderState);
        leaderStatus.setAction(action);
        leaderStatus.setLeaderId(leaderId);
    }

    @Override
    public Pipe.CommandMessage getMessage() {
        commandBuilder.setLeaderStatus(leaderStatus);
        return commandBuilder.build();
    }
}
