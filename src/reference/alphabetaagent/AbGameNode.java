package at.ac.tuwien.ifs.sge.agent.alphabetaagent;

import java.util.Comparator;
import java.util.HashMap;
import at.ac.tuwien.ifs.sge.game.Game;
import java.util.Map;
import at.ac.tuwien.ifs.sge.util.node.GameNode;

public class AbGameNode<A> implements GameNode<A>
{
    private final Map<A, Integer> actionFrequency;
    private Game<A, ?> game;
    private double utility;
    private double heuristic;
    private int absoluteDepth;
    private boolean evaluated;

    public AbGameNode() {
        this(null, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0);
    }

    public AbGameNode(final Game<A, ?> game, final A action, final double[] weights, final int absoluteDepth) {
        this(game.doAction((Object)action), weights, absoluteDepth);
    }

    public AbGameNode(final Game<A, ?> game, final double[] weights, final int absoluteDepth) {
        this(game, Double.NEGATIVE_INFINITY * ((0 <= game.getCurrentPlayer() && game.getCurrentPlayer() < game.getNumberOfPlayers()) ? weights[game.getCurrentPlayer()] : -1.0), Double.NEGATIVE_INFINITY * ((0 <= game.getCurrentPlayer() && game.getCurrentPlayer() < game.getNumberOfPlayers()) ? weights[game.getCurrentPlayer()] : -1.0), absoluteDepth);
    }

    public AbGameNode(final Game<A, ?> game, final double utility, final double heuristic, final int absoluteDepth) {
        this.game = game;
        this.utility = utility;
        this.heuristic = heuristic;
        this.absoluteDepth = absoluteDepth;
        this.evaluated = false;
        if (game != null && game.getCurrentPlayer() < 0) {
            this.actionFrequency = new HashMap<A, Integer>();
        }
        else {
            this.actionFrequency = null;
        }
    }

    public Game<A, ?> getGame() {
        return this.game;
    }

    public void setGame(final Game<A, ?> game) {
        this.game = game;
    }

    public double getUtility() {
        return this.utility;
    }

    public void setUtility(final double utility) {
        this.utility = utility;
    }

    public double getHeuristic() {
        return this.heuristic;
    }

    public void setHeuristic(final double heuristic) {
        this.heuristic = heuristic;
    }

    public int getAbsoluteDepth() {
        return this.absoluteDepth;
    }

    public void setAbsoluteDepth(final int absoluteDepth) {
        this.absoluteDepth = absoluteDepth;
    }

    public boolean isEvaluated() {
        return this.evaluated;
    }

    public void setEvaluated(final boolean evaluated) {
        this.evaluated = evaluated;
    }

    public void simulateDetermineAction() {
        final A action = (A)this.game.determineNextAction();
        if (action != null && this.actionFrequency != null) {
            this.actionFrequency.compute(action, (k, v) -> (v == null) ? 1 : (v + 1));
        }
    }

    public void simulateDetermineAction(final int times) {
        for (int i = 0; i < times; ++i) {
            this.simulateDetermineAction();
        }
    }

    public boolean areSimulationDone() {
        return this.actionFrequency != null && !this.actionFrequency.isEmpty();
    }

    public int simulationsDone() {
        if (this.actionFrequency == null) {
            return 0;
        }
        return this.actionFrequency.values().stream().reduce(0, Integer::sum);
    }

    public double frequencyOf(final A action) {
        if (this.actionFrequency == null) {
            return 1.0;
        }
        final double all = this.simulationsDone();
        final double n = this.actionFrequency.getOrDefault(action, 0);
        return n / all;
    }

    public A mostFrequentAction() {
        A action = null;
        if (this.actionFrequency != null) {
            final Map.Entry<A, Integer> frequency = this.actionFrequency.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
            if (frequency != null) {
                action = frequency.getKey();
            }
        }
        return action;
    }

    public boolean isMostFrequentAction(final A action) {
        if (this.actionFrequency != null) {
            final int frequency = this.actionFrequency.getOrDefault(action, 0);
            return this.actionFrequency.values().stream().noneMatch(f -> f > frequency);
        }
        return false;
    }
}
