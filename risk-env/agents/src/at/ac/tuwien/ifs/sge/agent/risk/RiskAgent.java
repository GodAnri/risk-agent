package at.ac.tuwien.ifs.sge.agent.risk;

import at.ac.tuwien.ifs.sge.game.Game;
import java.util.Iterator;
import java.util.Set;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import java.util.concurrent.TimeUnit;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;

public class RiskAgent extends AbstractGameAgent<Risk, RiskAction> implements GameAgent<Risk, RiskAction>
{
    public RiskAgent(final Logger log) {
        super(0.75, 5L, TimeUnit.SECONDS, log);
    }
    
    public void setUp(final int numberOfPlayers, final int playerId) {
        super.setUp(numberOfPlayers, playerId);
    }
    
    public RiskAction computeNextAction(final Risk game, final long computationTime, final TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit);
        this.nanosElapsed();
        this.nanosLeft();
        this.shouldStopComputation();
        final RiskBoard board = game.getBoard();
        board.getNrOfTerritoriesOccupiedByPlayer(this.playerId);
        game.getHeuristicValue(new double[0]);
        game.getHeuristicValue(this.playerId);
        final Set<RiskAction> possibleActions = (Set<RiskAction>)game.getPossibleActions();
        double bestUtilityValue = Double.NEGATIVE_INFINITY;
        double bestHeuristicValue = Double.NEGATIVE_INFINITY;
        RiskAction bestAction = null;
        for (final RiskAction possibleAction : possibleActions) {
            final Risk next = (Risk)game.doAction(possibleAction);
            final double nextUtilityValue = next.getUtilityValue(this.playerId);
            final double nextHeuristicValue = next.getHeuristicValue(this.playerId);
            if (bestUtilityValue <= nextUtilityValue && (bestUtilityValue < nextUtilityValue || bestHeuristicValue <= nextHeuristicValue)) {
                bestUtilityValue = nextUtilityValue;
                bestHeuristicValue = nextHeuristicValue;
                bestAction = possibleAction;
            }
        }
        assert bestAction != null;
        assert game.isValidAction(bestAction);
        this.log.debugf("Found best move: %s", new Object[] { bestAction.toString() });
        return bestAction;
    }
    
    public void tearDown() {
    }
    
    public void destroy() {
    }
}
