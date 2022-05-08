package at.ac.tuwien.ifs.sge.agent.riskyordealagent;

import at.ac.tuwien.ifs.sge.game.Game;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import java.util.concurrent.TimeUnit;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;

public class RiskyOrdealAgent extends AbstractGameAgent<Risk, RiskAction> implements GameAgent<Risk, RiskAction>
{
    public RiskyOrdealAgent(final Logger log) {
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
//        RiskAction bestAction = getBestAction(this.playerId, game);
        RiskAction bestAction = getRandomAction(game);
        assert bestAction != null;
        assert game.isValidAction(bestAction);
        this.log.debugf("Found best move: %s", new Object[] { bestAction.toString() });
        return bestAction;
    }

    public RiskAction getRandomAction(final Risk game){
        final Set<RiskAction> possibleActions = (Set<RiskAction>)game.getPossibleActions();
        RiskAction bestAction = null;
        int item = new Random().nextInt(possibleActions.size());
        int i = 0;
        for(RiskAction action : possibleActions)
        {
            if (i == item) {
                bestAction = action;
                break;
            }
            i++;
        }
        return bestAction;
    }

    public RiskAction getBestAction(int playerid, final Risk game){
        final Set<RiskAction> possibleActions = (Set<RiskAction>)game.getPossibleActions();
        double bestUtilityValue = Double.NEGATIVE_INFINITY;
        double bestHeuristicValue = Double.NEGATIVE_INFINITY;
        RiskAction bestAction = null;
            for (final RiskAction possibleAction : possibleActions) {
                final Risk next = (Risk)game.doAction(possibleAction);
                final double nextUtilityValue = next.getUtilityValue(playerid);
                final double nextHeuristicValue = getHeuristicValue(this.playerId, game, next);
//                final double nextHeuristicValue = getPathHeuristicValue(playerid, game, next, 0, 5);
//            System.out.println((Risk)next.getActionRecords().get(0));
//            final double nextHeuristicValue = next.getHeuristicValue(this.playerId);
//            System.out.println("Possible Action:" + possibleAction + " | Next Action:" + next + " | Heuristic Value:" + nextHeuristicValue + " | Utility Value:" + nextUtilityValue);
//            System.out.println(game.getBoard().getContinents());
//            for (int i = 0; i <= )
                if (bestUtilityValue <= nextUtilityValue && (bestUtilityValue < nextUtilityValue || bestHeuristicValue <= nextHeuristicValue)) {    //Maybe remove = sign in the bestHeauristic value?
                    bestUtilityValue = nextUtilityValue;
                    bestHeuristicValue = nextHeuristicValue;
                    bestAction = possibleAction;
                }
            }
        return bestAction;
    }

    public double getPathHeuristicValue(int playerid, final Risk game, final Risk nextActionState, double hv, int depth){
        double heuristicValue = hv;
        if (depth > 0) {
            heuristicValue += getHeuristicValue(playerid, game, nextActionState);
            final Set<RiskAction> possibleActions = (Set<RiskAction>)nextActionState.getPossibleActions();
            for (final RiskAction possibleAction : possibleActions) {
                final Risk nextNextActionState = (Risk)game.doAction(possibleAction);
                heuristicValue += getPathHeuristicValue(playerid, nextActionState, nextNextActionState, heuristicValue, depth - 1);
                System.out.println("Heauristic" + heuristicValue + " depth" + depth);
            }
            System.out.println("Heauristic" + heuristicValue + " depth" + depth);
            return heuristicValue;
        }
        System.out.println("Heauristic" + heuristicValue + " depth" + depth);
        return 0;
    }

    public double getHeuristicValue(int playerid, final Risk previousActionState, final Risk currentActionState){
        double heuristicValue = 0;
        RiskBoard currentBoard = currentActionState.getBoard();
        if (currentBoard.isReinforcementPhase()){

        }
        else if(currentBoard.isAttackPhase()){

        }
        else if(currentBoard.isFortifyPhase()){

        }
        else if(currentBoard.isOccupyPhase()){

        }
        if (previousActionState.getBoard().getNrOfTerritoriesOccupiedByPlayer(playerid) < currentActionState.getBoard().getNrOfTerritoriesOccupiedByPlayer(playerid))
            heuristicValue++;
        if  (currentActionState.getBoard().couldTradeInCards(playerid)){
            heuristicValue = heuristicValue + currentActionState.getBoard().getTradeInBonus();
        }
//        System.out.println(heuristicValue);
        return heuristicValue;
    }

    public void tearDown() {
    }

    public void destroy() {
    }
}
