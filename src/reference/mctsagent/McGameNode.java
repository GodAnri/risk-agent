package at.ac.tuwien.ifs.sge.agent.mctsagent;

import java.util.Objects;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.util.node.GameNode;

public class McGameNode<A> implements GameNode<A>
{
    private Game<A, ?> game;
    private int wins;
    private int plays;

    public McGameNode() {
        this(null);
    }

    public McGameNode(final Game<A, ?> game) {
        this(game, 0, 0);
    }

    public McGameNode(final Game<A, ?> game, final A action) {
        this(game.doAction((Object)action));
    }

    public McGameNode(final Game<A, ?> game, final int wins, final int plays) {
        this.game = game;
        this.wins = wins;
        this.plays = plays;
    }

    public Game<A, ?> getGame() {
        return this.game;
    }

    public void setGame(final Game<A, ?> game) {
        this.game = game;
    }

    public int getWins() {
        return this.wins;
    }

    public void setWins(final int wins) {
        this.wins = wins;
    }

    public void incWins() {
        ++this.wins;
    }

    public int getPlays() {
        return this.plays;
    }

    public void setPlays(final int plays) {
        this.plays = plays;
    }

    public void incPlays() {
        ++this.plays;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final McGameNode<?> mcGameNode = (McGameNode<?>)o;
        return this.wins == mcGameNode.wins && this.plays == mcGameNode.plays && this.game.equals(mcGameNode.game);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.game, this.wins, this.plays);
    }
}
