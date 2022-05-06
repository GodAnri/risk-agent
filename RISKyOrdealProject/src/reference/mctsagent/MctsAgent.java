//package at.ac.tuwien.ifs.sge.agent.mctsagent;
//
//import java.util.Set;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import at.ac.tuwien.ifs.sge.util.Util;
//import java.util.concurrent.TimeUnit;
//import at.ac.tuwien.ifs.sge.util.tree.DoubleLinkedTree;
//import at.ac.tuwien.ifs.sge.engine.Logger;
//import at.ac.tuwien.ifs.sge.util.tree.Tree;
//import java.util.Comparator;
//import at.ac.tuwien.ifs.sge.agent.GameAgent;
//import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
//import at.ac.tuwien.ifs.sge.game.Game;
//
//public class MctsAgent<G extends Game<A, ?>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A>
//{
//    private static final int MAX_PRINT_THRESHOLD = 97;
//    private static int INSTANCE_NR_COUNTER;
//    private final int instanceNr;
//    private final double exploitationConstant;
//    private Comparator<Tree<McGameNode<A>>> gameMcTreeUCTComparator;
//    private Comparator<Tree<McGameNode<A>>> gameMcTreeSelectionComparator;
//    private Comparator<Tree<McGameNode<A>>> gameMcTreePlayComparator;
//    private Comparator<McGameNode<A>> gameMcNodePlayComparator;
//    private Comparator<Tree<McGameNode<A>>> gameMcTreeWinComparator;
//    private Comparator<McGameNode<A>> gameMcNodeWinComparator;
//    private Comparator<Tree<McGameNode<A>>> gameMcTreeMoveComparator;
//    private Comparator<McGameNode<A>> gameMcNodeMoveComparator;
//    private Comparator<McGameNode<A>> gameMcNodeGameComparator;
//    private Comparator<Tree<McGameNode<A>>> gameMcTreeGameComparator;
//    private Tree<McGameNode<A>> mcTree;
//
//    public MctsAgent() {
//        this(null);
//    }
//
//    public MctsAgent(final Logger log) {
//        this(Math.sqrt(2.0), log);
//    }
//
//    public MctsAgent(final double exploitationConstant, final Logger log) {
//        super(log);
//        this.exploitationConstant = exploitationConstant;
//        this.mcTree = (at.ac.tuwien.ifs.sge.util.tree.Tree<McGameNode<A>>)new DoubleLinkedTree();
//        this.instanceNr = MctsAgent.INSTANCE_NR_COUNTER++;
//    }
//
//    public void setUp(final int numberOfPlayers, final int playerId) {
//        super.setUp(numberOfPlayers, playerId);
//        this.mcTree.clear();
//        this.mcTree.setNode((Object)new McGameNode());
//        this.gameMcTreeUCTComparator = Comparator.comparingDouble(t -> this.upperConfidenceBound(t, this.exploitationConstant));
//        this.gameMcNodePlayComparator = Comparator.comparingInt(McGameNode::getPlays);
//        this.gameMcTreePlayComparator = ((o1, o2) -> this.gameMcNodePlayComparator.compare((McGameNode<A>)o1.getNode(), (McGameNode<A>)o2.getNode()));
//        this.gameMcNodeWinComparator = Comparator.comparingInt(McGameNode::getWins);
//        this.gameMcTreeWinComparator = ((o1, o2) -> this.gameMcNodeWinComparator.compare((McGameNode<A>)o1.getNode(), (McGameNode<A>)o2.getNode()));
//        this.gameMcNodeGameComparator = ((o1, o2) -> this.gameComparator.compare(o1.getGame(), o2.getGame()));
//        this.gameMcTreeGameComparator = ((o1, o2) -> this.gameMcNodeGameComparator.compare((McGameNode<A>)o1.getNode(), (McGameNode<A>)o2.getNode()));
//        this.gameMcTreeSelectionComparator = this.gameMcTreeUCTComparator.thenComparing(this.gameMcTreeGameComparator);
//        this.gameMcNodeMoveComparator = this.gameMcNodePlayComparator.thenComparing(this.gameMcNodeWinComparator).thenComparing(this.gameMcNodeGameComparator);
//        this.gameMcTreeMoveComparator = ((o1, o2) -> this.gameMcNodeMoveComparator.compare((McGameNode<A>)o1.getNode(), (McGameNode<A>)o2.getNode()));
//    }
//
//    public A computeNextAction(final G game, final long computationTime, final TimeUnit timeUnit) {
//        super.setTimers(computationTime, timeUnit);
//        this.log.tra_("Searching for root of tree");
//        final boolean foundRoot = Util.findRoot((Tree)this.mcTree, (Game)game);
//        if (foundRoot) {
//            this.log._trace(", done.");
//        }
//        else {
//            this.log._trace(", failed.");
//        }
//        this.log.tra_("Check if best move will eventually end game: ");
//        if (this.sortPromisingCandidates(this.mcTree, this.gameMcNodeMoveComparator.reversed())) {
//            this.log._trace("Yes");
//            return (A)((McGameNode)Collections.max((Collection<? extends Tree>)this.mcTree.getChildren(), (Comparator<? super Tree>)this.gameMcTreeMoveComparator).getNode()).getGame().getPreviousAction();
//        }
//        this.log._trace("No");
//        int looped = 0;
//        this.log.debf_("MCTS with %d simulations at confidence %.1f%%", new Object[] { ((McGameNode)this.mcTree.getNode()).getPlays(), Util.percentage(((McGameNode)this.mcTree.getNode()).getWins(), ((McGameNode)this.mcTree.getNode()).getPlays()) });
//        int printThreshold = 1;
//        while (!this.shouldStopComputation()) {
//            if (looped++ % printThreshold == 0) {
//                this.log._deb_("\r");
//                this.log.debf_("MCTS with %d simulations at confidence %.1f%%", new Object[] { ((McGameNode)this.mcTree.getNode()).getPlays(), Util.percentage(((McGameNode)this.mcTree.getNode()).getWins(), ((McGameNode)this.mcTree.getNode()).getPlays()) });
//            }
//            Tree<McGameNode<A>> tree = this.mcTree;
//            tree = this.mcSelection(tree);
//            this.mcExpansion(tree);
//            final boolean won = this.mcSimulation(tree, 128, 2);
//            this.mcBackPropagation(tree, won);
//            if (printThreshold < 97) {
//                printThreshold = Math.max(1, Math.min(97, Math.round(((McGameNode)this.mcTree.getNode()).getPlays() * 11.111111f)));
//            }
//        }
//        final long elapsedTime = Math.max(1L, System.nanoTime() - this.START_TIME);
//        this.log._deb_("\r");
//        this.log.debf_("MCTS with %d simulations at confidence %.1f%%", new Object[] { ((McGameNode)this.mcTree.getNode()).getPlays(), Util.percentage(((McGameNode)this.mcTree.getNode()).getWins(), ((McGameNode)this.mcTree.getNode()).getPlays()) });
//        this.log._debugf(", done in %s with %s/simulation.", new Object[] { Util.convertUnitToReadableString(elapsedTime, TimeUnit.NANOSECONDS, timeUnit), Util.convertUnitToReadableString(elapsedTime / Math.max(1, ((McGameNode)this.mcTree.getNode()).getPlays()), TimeUnit.NANOSECONDS, TimeUnit.NANOSECONDS) });
//        if (this.mcTree.isLeaf()) {
//            this.log._debug(". Could not find a move, choosing the next best greedy option.");
//            return Collections.max((Collection<? extends A>)game.getPossibleActions(), (o1, o2) -> this.gameComparator.compare(game.doAction(o1), game.doAction(o2)));
//        }
//        return (A)((McGameNode)Collections.max((Collection<? extends Tree>)this.mcTree.getChildren(), (Comparator<? super Tree>)this.gameMcTreeMoveComparator).getNode()).getGame().getPreviousAction();
//    }
//
//    private boolean sortPromisingCandidates(Tree<McGameNode<A>> tree, final Comparator<McGameNode<A>> comparator) {
//        boolean isDetermined;
//        for (isDetermined = true; !tree.isLeaf() && isDetermined; tree = (Tree<McGameNode<A>>)tree.getChild(0)) {
//            isDetermined = tree.getChildren().stream().allMatch(c -> ((McGameNode)c.getNode()).getGame().getCurrentPlayer() >= 0);
//            if (((McGameNode)tree.getNode()).getGame().getCurrentPlayer() == this.playerId) {
//                tree.sort((Comparator)comparator);
//            }
//            else {
//                tree.sort((Comparator)comparator.reversed());
//            }
//        }
//        return isDetermined && ((McGameNode)tree.getNode()).getGame().isGameOver();
//    }
//
//    private Tree<McGameNode<A>> mcSelection(Tree<McGameNode<A>> tree) {
//        int depth = 0;
//        while (!tree.isLeaf() && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
//            final List<Tree<McGameNode<A>>> children = new ArrayList<Tree<McGameNode<A>>>(tree.getChildren());
//            if (((McGameNode)tree.getNode()).getGame().getCurrentPlayer() < 0) {
//                final A action = (A)((McGameNode)tree.getNode()).getGame().determineNextAction();
//                for (final Tree<McGameNode<A>> child : children) {
//                    if (((McGameNode)child.getNode()).getGame().getPreviousAction().equals(action)) {
//                        tree = child;
//                        break;
//                    }
//                }
//            }
//            else {
//                tree = (Tree<McGameNode<A>>)Collections.max((Collection<? extends Tree>)children, (Comparator<? super Tree>)this.gameMcTreeSelectionComparator);
//            }
//        }
//        return tree;
//    }
//
//    private void mcExpansion(final Tree<McGameNode<A>> tree) {
//        if (tree.isLeaf()) {
//            final Game<A, ?> game = ((McGameNode)tree.getNode()).getGame();
//            final Set<A> possibleActions = (Set<A>)game.getPossibleActions();
//            for (final A possibleAction : possibleActions) {
//                tree.add((Object)new McGameNode((at.ac.tuwien.ifs.sge.game.Game<Object, ?>)game, possibleAction));
//            }
//        }
//    }
//
//    private boolean mcSimulation(final Tree<McGameNode<A>> tree, final int simulationsAtLeast, final int proportion) {
//        final int simulationsDone = ((McGameNode)tree.getNode()).getPlays();
//        if (simulationsDone < simulationsAtLeast && this.shouldStopComputation(proportion)) {
//            final int simulationsLeft = simulationsAtLeast - simulationsDone;
//            return this.mcSimulation(tree, this.nanosLeft() / simulationsLeft);
//        }
//        if (simulationsDone == 0) {
//            return this.mcSimulation(tree, this.TIMEOUT / 2L - this.nanosElapsed());
//        }
//        return this.mcSimulation(tree);
//    }
//
//    private boolean mcSimulation(final Tree<McGameNode<A>> tree) {
//        Game<A, ?> game = ((McGameNode)tree.getNode()).getGame();
//        int depth = 0;
//        while (!game.isGameOver() && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
//            if (game.getCurrentPlayer() < 0) {
//                game = (Game<A, ?>)game.doAction();
//            }
//            else {
//                game = (Game<A, ?>)game.doAction(Util.selectRandom((Collection)game.getPossibleActions(), this.random));
//            }
//        }
//        return this.mcHasWon(game);
//    }
//
//    private boolean mcSimulation(final Tree<McGameNode<A>> tree, final long timeout) {
//        final long startTime = System.nanoTime();
//        Game<A, ?> game = ((McGameNode)tree.getNode()).getGame();
//        int depth = 0;
//        while (!game.isGameOver() && System.nanoTime() - startTime <= timeout && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
//            if (game.getCurrentPlayer() < 0) {
//                game = (Game<A, ?>)game.doAction();
//            }
//            else {
//                game = (Game<A, ?>)game.doAction(Util.selectRandom((Collection)game.getPossibleActions(), this.random));
//            }
//        }
//        return this.mcHasWon(game);
//    }
//
//    private boolean mcHasWon(final Game<A, ?> game) {
//        double[] evaluation = game.getGameUtilityValue();
//        double score = Util.scoreOutOfUtility(evaluation, this.playerId);
//        if (!game.isGameOver() && score > 0.0) {
//            evaluation = game.getGameHeuristicValue();
//            score = Util.scoreOutOfUtility(evaluation, this.playerId);
//        }
//        boolean win = score == 1.0;
//        final boolean tie = score > 0.0;
//        win = (win || (tie && this.random.nextBoolean()));
//        return win;
//    }
//
//    private void mcBackPropagation(Tree<McGameNode<A>> tree, final boolean win) {
//        int depth = 0;
//        while (!tree.isRoot() && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
//            tree = (Tree<McGameNode<A>>)tree.getParent();
//            ((McGameNode)tree.getNode()).incPlays();
//            if (win) {
//                ((McGameNode)tree.getNode()).incWins();
//            }
//        }
//    }
//
//    private double upperConfidenceBound(final Tree<McGameNode<A>> tree, final double c) {
//        final double w = ((McGameNode)tree.getNode()).getWins();
//        double N;
//        final double n = N = Math.max(((McGameNode)tree.getNode()).getPlays(), 1);
//        if (!tree.isRoot()) {
//            N = ((McGameNode)tree.getParent().getNode()).getPlays();
//        }
//        return w / n + c * Math.sqrt(Math.log(N) / n);
//    }
//
//    public String toString() {
//        if (this.instanceNr > 1 || MctsAgent.INSTANCE_NR_COUNTER > 2) {
//            return String.format("%s%d", "MctsAgent#", this.instanceNr);
//        }
//        return "MctsAgent";
//    }
//
//    static {
//        MctsAgent.INSTANCE_NR_COUNTER = 1;
//    }
//}
