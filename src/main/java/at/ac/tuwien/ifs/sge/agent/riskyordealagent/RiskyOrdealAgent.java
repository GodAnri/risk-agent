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
                System.out.println("That's the territory " + game.getBoard().getTerritories().get(bestAction.selected()));
                System.out.println("Selected territory " + bestAction.selected());
                System.out.println("Reinforced territory id " + bestAction.reinforcedId());
                System.out.println("Attacked territory id " + bestAction.attackingId());
                System.out.println("Defended territory id " + bestAction.defendingId());
                System.out.println("Fortified territory id " + bestAction.fortifiedId());
                System.out.println("Fortifying territory id " + bestAction.fortifyingId());
                System.out.println("Troops " + bestAction.troops());
//                System.out.println("Occupied territory id " + bestAction);
                if (game.getBoard().isReinforcementPhase()) {
                    r = r+1;
                    System.out.println("Phase: Reinforcement");
                }
                else if(game.getBoard().isAttackPhase()){
                    a = a+1;
                    System.out.println("Phase: Attack");
                }
                else if(game.getBoard().isFortifyPhase()){
                    f = f+1;
                    System.out.println("Phase: Fortify");
                }
                else if(game.getBoard().isOccupyPhase()){
                    o = o+1;
                    System.out.println("Phase: Occupy");
                }
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
        int counter = 0;
        System.out.println("There are " + possibleActions.size() + " possible actions in the roster");
        for (final RiskAction possibleAction : possibleActions) {
            possibleAction.attackingId();
            final Risk next = (Risk)game.doAction(possibleAction);
            final double nextUtilityValue = next.getUtilityValue(this.playerId);
            final double nextHeuristicValue = getHeuristicValue(game, next, possibleAction, counter);
            counter += 1;
//            final double nextHeuristicValue = getTurnHeuristicValue(game, next, 0, 5);
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
        final Risk next = (Risk)game.doAction(bestAction);
//        getHeuristicValue(game, next, bestAction, counter);
        printStatus(next, bestAction, bestHeuristicValue);
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
    public double getHeuristicValue(final Risk previousActionState, final Risk currentActionState, final RiskAction action, int counter){
        double heuristicValue = 0;
        RiskBoard currentBoard = currentActionState.getBoard();
        RiskBoard previousBoard = previousActionState.getBoard();

//        printStatus(currentActionState, action);
//        System.out.println("This is action number " + counter);

        //Different phases should involve different tactics
        if (action.isEndPhase())
            return 0;
        else if (currentBoard.isReinforcementPhase()) {
            if (action.reinforcedId() < 0)
                return Double.NEGATIVE_INFINITY;
            RiskTerritory reinforcedTerritory = currentBoard.getTerritories().get(action.reinforcedId());
            RiskTerritory beforeTerritory = previousBoard.getTerritories().get(action.reinforcedId());
            if (beforeTerritory.getTroops() == 0) {
                //Initial reinforcement:
                heuristicValue += 50;
                if (!currentBoard.neighboringFriendlyTerritories(action.reinforcedId()).isEmpty()) {
                    heuristicValue += 20;
                }
                if (currentBoard.neighboringEnemyTerritories(action.reinforcedId()).isEmpty()) {
                    heuristicValue += 10;
                }
                //Place soldiers in countries that are easily defendable (i.e. with least possible bordering countries)
                heuristicValue -= currentBoard.neighboringEnemyTerritories(action.reinforcedId()).size() * 5;
                //Initially place the soldiers so that they could capture or be very close to capturing a continent as a whole, or several of them to get bonuses quickly
                int continent = reinforcedTerritory.getContinentId();
                boolean conflict = false;
                for (int i = 0; i < 42; i++) {
                    RiskTerritory territory = currentBoard.getTerritories().get(i);
                    if (territory.getContinentId() == continent && territory.getOccupantPlayerId() != this.playerId && territory.getOccupantPlayerId() >= 0) {
                        conflict = true;
                    }
                }
                if (!conflict) {
                    heuristicValue += 10;
                }
            } else {
                //Try to not have any territory with low troops on the border
                for (int enemyID : currentBoard.neighboringEnemyTerritories(action.reinforcedId())) {
                    RiskTerritory enemy = currentBoard.getTerritories().get(enemyID);
                    double troopsAdded = reinforcedTerritory.getTroops() - beforeTerritory.getTroops();
                    int troopsAvailable = previousBoard.getNrOfTerritoriesOccupiedByPlayer(this.playerId)/3;
                    int troopDiff = reinforcedTerritory.getTroops() - enemy.getTroops();
                    if (troopsAdded >= 0.75 * troopsAvailable)
                        heuristicValue -= 200;
                    if (troopDiff > 100)
                        heuristicValue -= 50;
                    else heuristicValue += 2 * troopDiff;
                }
                //If a territory has low troops on the border, and it's not worth protecting, protect its neighbours instead
                for (int neighbourID : currentBoard.neighboringFriendlyTerritories(action.reinforcedId())) {
                    RiskTerritory neighbour = currentBoard.getTerritories().get(neighbourID);
                    for (int enemyID : currentBoard.neighboringEnemyTerritories(neighbourID)) {
                        RiskTerritory enemy = currentBoard.getTerritories().get(enemyID);
                        double troopsAdded = reinforcedTerritory.getTroops() - beforeTerritory.getTroops();
                        int troopsAvailable = previousBoard.getNrOfTerritoriesOccupiedByPlayer(this.playerId)/3;
                        int troopDiff = neighbour.getTroops() - enemy.getTroops();
                        if (troopsAdded >= 0.5 * troopsAvailable)
                            heuristicValue -= 200;
                        if (troopDiff < 0) {
                            int troopModifier = reinforcedTerritory.getTroops() + neighbour.getTroops() - enemy.getTroops();
                            heuristicValue += troopModifier;
                        } else heuristicValue -= 200;
                    }
                }
                boolean layer2 = false;
                if (currentBoard.neighboringEnemyTerritories(action.reinforcedId()).isEmpty()) {
                    for (int neighbourID : currentBoard.neighboringEnemyTerritories(action.reinforcedId())) {
                        if (!currentBoard.neighboringEnemyTerritories(neighbourID).isEmpty())
                            layer2 = true;
                    }
                    if (!layer2) {
                        heuristicValue -= 400;
                    }
                }
            }
            r = r+1;
        }
        else if(currentBoard.isAttackPhase()) {
            if (action.attackingId() < 0 || action.defendingId() < 0)
                return Double.NEGATIVE_INFINITY;
            RiskTerritory attackingTerritory = previousBoard.getTerritories().get(action.attackingId());
            RiskTerritory defendingTerritory = previousBoard.getTerritories().get(action.defendingId());
            if (attackingTerritory != null && defendingTerritory != null){
                //Penalty for each adjacent friendly territory to the owner, therefore our enemy's
                heuristicValue -= (previousBoard.neighboringFriendlyTerritories(action.defendingId()).size()) * 2.5;
                if (attackingTerritory.getTroops() >= defendingTerritory.getTroops())
                    heuristicValue += 100 * action.troops();
                else if (attackingTerritory.getTroops() < defendingTerritory.getTroops())
                    heuristicValue += 50 * action.troops() - 200;
                if (previousActionState.getBoard().getNrOfTerritoriesOccupiedByPlayer(this.playerId) < currentActionState.getBoard().getNrOfTerritoriesOccupiedByPlayer(this.playerId))
                    heuristicValue += 100;
            }
            a = a+1;
        }
        else if(currentBoard.isFortifyPhase()){
            if (action.fortifiedId() < 0 || action.fortifyingId() < 0)
                return Double.NEGATIVE_INFINITY;
            RiskTerritory fortifyingTerritory = currentBoard.getTerritories().get(action.fortifyingId());
            RiskTerritory beforeFortifying = previousBoard.getTerritories().get(action.fortifyingId());
            RiskTerritory fortifiedTerritory = currentBoard.getTerritories().get(action.fortifiedId());
            if (fortifyingTerritory != null && fortifiedTerritory != null){
                heuristicValue += currentBoard.neighboringEnemyTerritories(action.fortifiedId()).size();
                for (int enemy : currentBoard.neighboringEnemyTerritories(action.fortifiedId())){
                    if (fortifiedTerritory.getTroops() >= currentBoard.getTerritories().get(enemy).getTroops()){
                        heuristicValue += 10;
                    } else {
                        heuristicValue -= 15;
                    }
                }
                for (int enemy : currentBoard.neighboringEnemyTerritories(action.fortifyingId())){
                    if (fortifyingTerritory.getTroops() >= currentBoard.getTerritories().get(enemy).getTroops()){
                        heuristicValue += 10;
                    } else {
                        heuristicValue -= 25;
                    }
                }

                //Reward for "suiciding" lost territories to protect their neighbours
                for (int enemy : currentBoard.neighboringEnemyTerritories(action.fortifyingId())){
                    if (beforeFortifying.getTroops() < currentBoard.getTerritories().get(enemy).getTroops()) {
                        heuristicValue += 10 * action.troops();
                        // Reward for baiting into a more protected second layer
                        for (int friendly : currentBoard.neighboringFriendlyTerritories(action.fortifyingId())) {
                            heuristicValue += (currentBoard.getTerritories().get(friendly).getTroops() - currentBoard.getTerritories().get(enemy).getTroops()) * 5;
                        }
                    }
                }
            }
            f = f+1;
        }
        else if(currentBoard.isOccupyPhase()) {
            heuristicValue += 1000;
            o = o + 1;
        }
        if  (currentActionState.getBoard().couldTradeInCards(this.playerId)){
            heuristicValue = heuristicValue + currentActionState.getBoard().getTradeInBonus();
        }
        return heuristicValue;
    }

    public void printStatus(final Risk currentActionState, final RiskAction action, final double heuristicValue){
        RiskBoard currentBoard = currentActionState.getBoard();
        System.out.println("=====================ACTION==========================");
        System.out.println("Selected territory " + action.selected());
        System.out.println("Reinforced territory id " + action.reinforcedId());
        System.out.println("Attacked territory id " + action.attackingId());
        System.out.println("Defended territory id " + action.defendingId());
        System.out.println("Fortified territory id " + action.fortifiedId());
        System.out.println("Fortifying territory id " + action.fortifyingId());
        System.out.println("Troops are " + action.troops());
        System.out.println("====================================================");
        System.out.println("Heuristic value of the action is " + heuristicValue);
        System.out.println("====================================================");
        if (action.isEndPhase()){
            System.out.println("Phase: End Phase");
        }
        else if (currentBoard.isReinforcementPhase()) {
            r = r+1;
            System.out.println("Phase: Reinforcement");
            System.out.println("The territory with an id of " + action.selected() + " has been reinforced by " + action.troops() + " troops");
        }
        else if(currentBoard.isAttackPhase()){
            a = a+1;
            System.out.println("Phase: Attack");
            System.out.println("The territory with an id of " + action.attackingId() + " has attacked the territory with an id of " + action.defendingId() + " by using " + action.troops() +
                    " troops. The attacker has lost " + action.attackerCasualties() + " troops and the defendant has lost " + action.defenderCasualties());
        }
        else if(currentBoard.isFortifyPhase()){
            f = f+1;
            System.out.println("Phase: Fortify");
            System.out.println("The territory with an id of " + action.fortifyingId() + " has fortified the territory with an id of " + action.fortifiedId() + " by using " + action.troops() +
                    " troops.");
        }
        else if(currentBoard.isOccupyPhase()){
            o = o+1;
            System.out.println("Phase: Occupy");
            System.out.println("The territory with an id of " + currentActionState.getPreviousAction().defendingId() + " has been occupied by using " + action.troops() +
                    " troops.");
        }
    }

    public void tearDown() {
    }

    public void destroy() {
    }
}