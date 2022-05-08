package at.ac.tuwien.ifs.sge.agent.randomagent;

import java.util.Collection;
import at.ac.tuwien.ifs.sge.util.Util;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.game.Game;

public class RandomAgent<G extends Game<A, ?>, A> implements GameAgent<G, A>
{
    private static int INSTANCE_NR_COUNTER;
    private final Logger log;
    private final Random random;
    private final int instanceNr;

    public RandomAgent() {
        this(new Random(), null);
    }

    public RandomAgent(final Logger log) {
        this(new Random(), log);
    }

    public RandomAgent(final Random random, final Logger log) {
        this.random = random;
        this.log = log;
        this.instanceNr = RandomAgent.INSTANCE_NR_COUNTER++;
    }

    public A computeNextAction(final G game, final long computationTime, final TimeUnit timeUnit) {
        return (A)Util.selectRandom((Collection)game.getPossibleActions(), this.random);
    }

    @Override
    public String toString() {
        if (this.instanceNr > 1 || RandomAgent.INSTANCE_NR_COUNTER > 2) {
            return String.format("%s%d", "RandomAgent#", this.instanceNr);
        }
        return "RandomAgent";
    }

    static {
        RandomAgent.INSTANCE_NR_COUNTER = 1;
    }
}
