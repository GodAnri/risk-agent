package at.ac.tuwien.ifs.sge.agent.riskyordealagent;

import java.util.Random;
import java.util.Set;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import java.util.concurrent.TimeUnit;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;

/*
    TODO:
        [] Create a heuristic function
            [] Counts the value accordingly depending on the state of the current turn
            [] Has the rules that strive to find the best possible next action
            [] Think of the number of actions/turns we want the heuristic value to be calculated for
*/

public class RiskyOrdealAgent extends AbstractGameAgent<Risk, RiskAction> implements GameAgent<Risk, RiskAction>
{
    int r = 0;
    int a = 0;
    int f = 0;
    int o = 0;

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
        RiskAction bestAction = getBestAction(game);
//        RiskAction bestAction = getRandomAction(game);
        assert bestAction != null;
        assert game.isValidAction(bestAction);
        this.log.debugf("Found best move: %s", new Object[] { bestAction.toString() });
        return bestAction;
    }

    //A method just to test if the agent works by picking random actions
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

    //The part for getting the best action has been partitioned into its own method for better code readability
    public RiskAction getBestAction(final Risk game){
        final Set<RiskAction> possibleActions = (Set<RiskAction>)game.getPossibleActions();
        double bestUtilityValue = Double.NEGATIVE_INFINITY;
        double bestHeuristicValue = Double.NEGATIVE_INFINITY;
        RiskAction bestAction = null;
        r = 0;
        a = 0;
        f = 0;
        o = 0;
        for (final RiskAction possibleAction : possibleActions) {
            final Risk next = (Risk)game.doAction(possibleAction);
            final double nextUtilityValue = next.getUtilityValue(this.playerId);
            final double nextHeuristicValue = getHeuristicValue(game, next);
//                final double nextHeuristicValue = getTurnHeuristicValue(game, next, 0, 5);
//            System.out.println((Risk)next.getActionRecords().get(0));
//            final double nextHeuristicValue = next.getHeuristicValue(this.playerId);
//            System.out.println("Possible Action:" + possibleAction + " | Next Action:" + next + " | Heuristic Value:" + nextHeuristicValue + " | Utility Value:" + nextUtilityValue);
//            System.out.println(game.getBoard().getContinents());
//            for (int i = 0; i <= )
            if (bestUtilityValue <= nextUtilityValue && (bestUtilityValue < nextUtilityValue || bestHeuristicValue <= nextHeuristicValue)) {
                bestUtilityValue = nextUtilityValue;
                bestHeuristicValue = nextHeuristicValue;
                bestAction = possibleAction;
            }
        }
        System.out.println("Reinforce: "+r+"\n"+"Attack:"+a+"\n"+"Fortify:"+f+"\n"+"Occupy:"+o+"\n");
        return bestAction;
    }

    //An unsuccessful attempt at getting all of the heuristic values for the player's turn through the recursion,
    //to understand the possible overall heuristic value of the given turn. The approach was too similar to the Alpha
    //Beta pruning, so we decided to drop it.

    /*public double getTurnHeuristicValue(final Risk game, final Risk nextActionState, double hv){
        double heuristicValue = hv;
        if (nextActionState.getCurrentPlayer() == this.playerId) {
            heuristicValue += getHeuristicValue(game, nextActionState);
            final Set<RiskAction> possibleActions = (Set<RiskAction>)nextActionState.getPossibleActions();
            for (final RiskAction possibleAction : possibleActions) {
                final Risk nextNextActionState = (Risk)game.doAction(possibleAction);
                heuristicValue += getTurnHeuristicValue(nextActionState, nextNextActionState, heuristicValue);
            }
            return heuristicValue;
        }
        return 0;
    }*/

    //Agent's new state heuristic value determining method
    public double getHeuristicValue(final Risk previousActionState, final Risk currentActionState){
        double heuristicValue = 0;
        RiskBoard currentBoard = currentActionState.getBoard();
        RiskBoard previousBoard = previousActionState.getBoard();
        //Use reinforcement learning to decide between using the best offensive or the best defensive approach (only after calculating the best ones)

        //Possible (unlikely) strategy: countering common tactics -> Con: could result in overfitting against specific
        //techniques but work poorly against others.

        //Different phases should involve different tactics
//        if (currentBoard.isReinforcementPhase()) {
//          Initially place the soldiers so that they could capture or be very close to capturing a continent as a whole, or several of them to get bonuses quickly
//          Place soldiers in countries that are easily defendable (i.e. with least possible bordering countries) but also try to choke enemy territories with least borders (i.e. place soldiers in the only bordering country(ies))
//          Try to have as least bordering enemy territorries as possible, creating big empire out of your territories with well fortified borders
//          Try to not have any territory with low troops "close" (how close?) to the border
//            for (RiskTerritory territory : currentBoard.getTerritories().values()){
//                if (currentActionState.getCurrentPlayer() == territory.getOccupantPlayerId()) {
//
//                }
//            }
//        }
//        else if(currentBoard.isAttackPhase()){
//          Don't overextend into the enemy territories to avoid being cut out and encircled
//          Don't be attacking an enemy territory unless you are outnumbering them by a lot
//
//        }
//        else if(currentBoard.isFortifyPhase()){
//
//        }
//        else if(currentBoard.isOccupyPhase()) {
//
//        }

        for (int i = 0; i < 42; i++){
            if (currentBoard.getTerritories().get(i).getOccupantPlayerId() != previousBoard.getTerritories().get(i).getOccupantPlayerId() ||
                    currentBoard.getTerritories().get(i).getTroops() != previousBoard.getTerritories().get(i).getTroops()){
                System.out.println("Territory affected: " + i);
            }
        }
        if (currentBoard.isReinforcementPhase()) {
            r = r+1;
        }
        else if(currentBoard.isAttackPhase()){
            a = a+1;
        }
        else if(currentBoard.isFortifyPhase()){
            f = f+1;
        }
        else if(currentBoard.isOccupyPhase()){
            o = o+1;
        }
        if (previousActionState.getBoard().getNrOfTerritoriesOccupiedByPlayer(this.playerId) < currentActionState.getBoard().getNrOfTerritoriesOccupiedByPlayer(this.playerId))
            heuristicValue++;
        if  (currentActionState.getBoard().couldTradeInCards(this.playerId)){
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
