package at.ac.tuwien.ifs.sge.agent.alphabetaagent;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import at.ac.tuwien.ifs.sge.util.Util;
import java.util.concurrent.TimeUnit;
import at.ac.tuwien.ifs.sge.util.tree.DoubleLinkedTree;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.util.tree.Tree;
import java.util.Comparator;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.game.Game;

public class AlphaBetaAgent<G extends Game<A, ?>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A>
{
    private static int INSTANCE_NR_COUNTER;
    private final int instanceNr;
    private final int maxDepth;
    private int lastDepth;
    private int depth;
    private Comparator<AbGameNode<A>> gameAbNodeUtilityComparator;
    private Comparator<AbGameNode<A>> gameAbNodeHeuristicComparator;
    private Comparator<AbGameNode<A>> gameAbNodeEvaluatedComparator;
    private Comparator<AbGameNode<A>> gameAbNodeComparator;
    private Comparator<AbGameNode<A>> gameAbNodeMoveComparator;
    private Comparator<Tree<AbGameNode<A>>> gameAbTreeComparator;
    private Tree<AbGameNode<A>> abTree;
    private int alphaCutOffs;
    private int betaCutOffs;
    private int excessTime;
    private int averageBranchingCount;
    private double averageBranching;
    
    public AlphaBetaAgent() {
        this(64, null);
    }
    
    public AlphaBetaAgent(final Logger log) {
        this(64, log);
    }
    
    public AlphaBetaAgent(final int maxDepth, final Logger log) {
        super(log);
        this.maxDepth = maxDepth;
        this.abTree = (at.ac.tuwien.ifs.sge.util.tree.Tree<AbGameNode<A>>)new DoubleLinkedTree();
        this.instanceNr = AlphaBetaAgent.INSTANCE_NR_COUNTER++;
    }
    
    public void setUp(final int numberOfPlayers, final int playerId) {
        super.setUp(numberOfPlayers, playerId);
        this.abTree.clear();
        this.abTree.setNode((Object)new AbGameNode());
        this.averageBranchingCount = 0;
        this.averageBranching = 10.0;
        this.gameAbNodeUtilityComparator = Comparator.comparingDouble(AbGameNode::getUtility);
        this.gameAbNodeHeuristicComparator = Comparator.comparingDouble(AbGameNode::getHeuristic);
        this.gameAbNodeEvaluatedComparator = ((o1, o2) -> Boolean.compare(o1.isEvaluated(), o2.isEvaluated()));
        this.gameAbNodeComparator = this.gameAbNodeUtilityComparator.thenComparing(this.gameAbNodeHeuristicComparator);
        this.gameAbNodeMoveComparator = this.gameAbNodeComparator.thenComparing((o1, o2) -> this.gameComparator.compare(o1.getGame(), o2.getGame()));
        this.gameAbTreeComparator = ((Comparator<Tree<AbGameNode<A>>>)((o1, o2) -> this.gameAbNodeEvaluatedComparator.compare((AbGameNode<A>)o1.getNode(), (AbGameNode<A>)o2.getNode()))).thenComparing((o1, o2) -> this.gameAbNodeMoveComparator.compare((AbGameNode<A>)o1.getNode(), (AbGameNode<A>)o2.getNode()));
    }
    
    public A computeNextAction(final G game, final long computationTime, final TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit);
        this.log.tra_("Searching for root of tree");
        final boolean foundRoot = Util.findRoot((Tree)this.abTree, (Game)game);
        if (foundRoot) {
            this.log._trace(", done.");
        }
        else {
            this.log._trace(", failed.");
        }
        this.log.tra_("Check if best move will eventually end game: ");
        if (this.sortPromisingCandidates(this.abTree, this.gameAbNodeComparator.reversed())) {
            this.log._trace("Yes");
            return (A)((AbGameNode)Collections.max((Collection<? extends Tree>)this.abTree.getChildren(), (Comparator<? super Tree>)this.gameAbTreeComparator).getNode()).getGame().getPreviousAction();
        }
        this.log._trace("No");
        this.lastDepth = 1;
        this.excessTime = 2;
        int labeled = 1;
        this.log.deb_("Labeling tree 1 time");
        while (!this.shouldStopComputation() && this.excessTime > 1 && labeled <= this.lastDepth) {
            this.depth = this.determineDepth();
            if (labeled > 1) {
                this.log._deb_("\r");
                this.log.deb_(invokedynamic(makeConcatWithConstants:(I)Ljava/lang/String;, labeled));
            }
            this.log._deb_(invokedynamic(makeConcatWithConstants:(I)Ljava/lang/String;, this.depth));
            this.alphaCutOffs = 0;
            this.betaCutOffs = 0;
            this.labelAlphaBetaTree(this.abTree, this.depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            this.excessTime = (int)(this.TIMEOUT / Math.min(Math.max(System.nanoTime() - this.START_TIME, 1L), this.TIMEOUT));
            ++labeled;
        }
        this.log._debugf(", done with %d alpha cut-off%s, %d beta cut-off%s and %s left.", new Object[] { this.alphaCutOffs, (this.alphaCutOffs != 1) ? "s" : "", this.betaCutOffs, (this.betaCutOffs != 1) ? "s" : "", Util.convertUnitToReadableString(this.ACTUAL_TIMEOUT - (System.nanoTime() - this.START_TIME), TimeUnit.NANOSECONDS, timeUnit) });
        this.log.tracef("Tree has %d nodes, maximum depth %d, and an average branching factor of %s", new Object[] { this.abTree.size(), this.depth, Util.convertDoubleToMinimalString(this.averageBranching, 2) });
        if (this.abTree.isLeaf()) {
            this.log.debug("Could not find a move, choosing the next best greedy option.");
            return Collections.max((Collection<? extends A>)game.getPossibleActions(), (o1, o2) -> this.gameComparator.compare(game.doAction(o1), game.doAction(o2)));
        }
        if (!((AbGameNode)this.abTree.getNode()).isEvaluated()) {
            this.labelMinMaxTree(this.abTree, 1);
        }
        this.log.debugf("Utility: %.1f, Heuristic: %.1f", new Object[] { ((AbGameNode)this.abTree.getNode()).getUtility(), ((AbGameNode)this.abTree.getNode()).getHeuristic() });
        return (A)((AbGameNode)Collections.max((Collection<? extends Tree>)this.abTree.getChildren(), (Comparator<? super Tree>)this.gameAbTreeComparator).getNode()).getGame().getPreviousAction();
    }
    
    private boolean expandNode(final Tree<AbGameNode<A>> tree) {
        if (tree.isLeaf()) {
            final AbGameNode<A> abGameNode = (AbGameNode<A>)tree.getNode();
            final Game<A, ?> game = abGameNode.getGame();
            if (!game.isGameOver()) {
                final Set<A> possibleActions = (Set<A>)game.getPossibleActions();
                this.averageBranching = (this.averageBranching * this.averageBranchingCount++ + possibleActions.size()) / this.averageBranchingCount;
                for (final A possibleAction : possibleActions) {
                    tree.add((Object)new AbGameNode((at.ac.tuwien.ifs.sge.game.Game<Object, ?>)game, possibleAction, this.minMaxWeights, abGameNode.getAbsoluteDepth() + 1));
                }
            }
        }
        return !tree.isLeaf();
    }
    
    private boolean appearsQuiet(final Tree<AbGameNode<A>> tree) {
        if (tree.isRoot()) {
            return true;
        }
        final List<Tree<AbGameNode<A>>> siblings = (List<Tree<AbGameNode<A>>>)tree.getParent().getChildren();
        final double min = ((AbGameNode)Collections.min((Collection<? extends Tree>)siblings, (Comparator<? super Tree>)this.gameAbTreeComparator).getNode()).getUtility();
        final double max = ((AbGameNode)Collections.max((Collection<? extends Tree>)siblings, (Comparator<? super Tree>)this.gameAbTreeComparator).getNode()).getUtility();
        return siblings.size() <= 2 || (min < ((AbGameNode)tree.getNode()).getGame().getUtilityValue(new double[0]) && ((AbGameNode)tree.getNode()).getGame().getUtilityValue(new double[0]) < max);
    }
    
    private void quiescence(Tree<AbGameNode<A>> tree) {
        final Tree<AbGameNode<A>> originalTree = tree;
        boolean isQuiet = false;
        AbGameNode<A> node;
        for (node = (AbGameNode<A>)tree.getNode(); !node.isEvaluated(); node = (AbGameNode<A>)tree.getNode()) {
            final Game<A, ?> game = node.getGame();
            if (game.isGameOver() || (game.getCurrentPlayer() >= 0 && (isQuiet || this.appearsQuiet(tree)))) {
                node.setUtility(game.getUtilityValue(this.minMaxWeights));
                node.setHeuristic(game.getHeuristicValue(this.minMaxWeights));
                node.setEvaluated(true);
            }
            else {
                this.expandNode(tree);
                tree.sort((Comparator)this.gameAbNodeComparator);
                tree = (Tree<AbGameNode<A>>)tree.getChild(tree.getChildren().size() / 2);
                isQuiet = true;
            }
        }
        final AbGameNode<A> originalNode = (AbGameNode<A>)originalTree.getNode();
        if (!originalNode.isEvaluated()) {
            originalNode.setUtility(node.getUtility());
            originalNode.setHeuristic(node.getHeuristic());
            originalNode.setEvaluated(true);
        }
    }
    
    private void evaluateNode(final Tree<AbGameNode<A>> tree) {
        final AbGameNode<A> node = (AbGameNode<A>)tree.getNode();
        if (tree.isLeaf()) {
            this.quiescence(tree);
        }
        if (!tree.isRoot()) {
            final AbGameNode<A> parent = (AbGameNode<A>)tree.getParent().getNode();
            final int parentCurrentPlayer = parent.getGame().getCurrentPlayer();
            final double utility = node.getUtility();
            final double heuristic = node.getHeuristic();
            if (!parent.isEvaluated()) {
                parent.setUtility(utility);
                parent.setHeuristic(heuristic);
            }
            else if (parentCurrentPlayer < 0) {
                final int nrOfSiblings = tree.getParent().getChildren().size();
                if (!parent.areSimulationDone()) {
                    parent.simulateDetermineAction(Math.max((int)Math.round(nrOfSiblings * this.simulationTimeFactor()), nrOfSiblings));
                }
                parent.simulateDetermineAction(nrOfSiblings);
                if (parent.isMostFrequentAction((A)node.getGame().getPreviousAction())) {
                    parent.setUtility(utility);
                    parent.setHeuristic(heuristic);
                }
            }
            else {
                final double parentUtility = parent.getUtility();
                final double parentHeuristic = parent.getHeuristic();
                if (parentCurrentPlayer == this.playerId) {
                    parent.setUtility(Math.max(parentUtility, utility));
                    parent.setHeuristic(Math.max(parentHeuristic, heuristic));
                }
                else {
                    parent.setUtility(Math.min(parentUtility, utility));
                    parent.setHeuristic(Math.min(parentHeuristic, heuristic));
                }
            }
            parent.setEvaluated(true);
        }
    }
    
    private void labelMinMaxTree(Tree<AbGameNode<A>> tree, int depth) {
        final Deque<Tree<AbGameNode<A>>> stack = new ArrayDeque<Tree<AbGameNode<A>>>();
        stack.push(tree);
        Tree<AbGameNode<A>> lastParent = null;
        depth = Math.max(((AbGameNode)tree.getNode()).getAbsoluteDepth() + depth, depth);
        int checkDepth = 0;
        while (!stack.isEmpty() && (checkDepth++ % 31 != 0 || !this.shouldStopComputation())) {
            tree = stack.peek();
            if (lastParent == tree || ((AbGameNode)tree.getNode()).getAbsoluteDepth() >= depth || !this.expandNode(tree)) {
                this.evaluateNode(tree);
                stack.pop();
                lastParent = (Tree<AbGameNode<A>>)tree.getParent();
            }
            else {
                this.pushChildrenOntoStack(tree, stack);
            }
        }
    }
    
    private void pushChildrenOntoStack(final Tree<AbGameNode<A>> tree, final Deque<Tree<AbGameNode<A>>> stack) {
        if (((AbGameNode)tree.getNode()).getGame().getCurrentPlayer() == this.playerId) {
            tree.sort((Comparator)this.gameAbNodeMoveComparator);
        }
        else {
            tree.sort((Comparator)this.gameAbNodeMoveComparator.reversed());
        }
        for (final Tree<AbGameNode<A>> child : tree.getChildren()) {
            stack.push(child);
        }
    }
    
    private void labelAlphaBetaTree(Tree<AbGameNode<A>> tree, int depth, double utilityAlpha, double utilityBeta, double heuristicAlpha, double heuristicBeta) {
        final Deque<Tree<AbGameNode<A>>> stack = new ArrayDeque<Tree<AbGameNode<A>>>();
        final Deque<Double> utilityAlphas = new ArrayDeque<Double>();
        final Deque<Double> heuristicAlphas = new ArrayDeque<Double>();
        final Deque<Double> utilityBetas = new ArrayDeque<Double>();
        final Deque<Double> heuristicBetas = new ArrayDeque<Double>();
        stack.push(tree);
        utilityAlphas.push(utilityAlpha);
        utilityBetas.push(utilityBeta);
        heuristicAlphas.push(heuristicAlpha);
        heuristicBetas.push(heuristicBeta);
        depth = Math.max(((AbGameNode)tree.getNode()).getAbsoluteDepth() + depth, depth);
        Tree<AbGameNode<A>> lastParent = null;
        int checkDepth = 0;
        while (!stack.isEmpty() && (checkDepth++ % 31 != 0 || !this.shouldStopComputation())) {
            tree = stack.peek();
            if (lastParent == tree || ((AbGameNode)tree.getNode()).getAbsoluteDepth() >= depth || !this.expandNode(tree)) {
                this.evaluateNode(tree);
                if (tree.isRoot() || ((AbGameNode)tree.getParent().getNode()).getGame().getCurrentPlayer() == this.playerId) {
                    utilityAlpha = Math.max(utilityAlphas.peek(), ((AbGameNode)tree.getNode()).getUtility());
                    heuristicAlpha = Math.max(heuristicAlphas.peek(), ((AbGameNode)tree.getNode()).getHeuristic());
                    utilityBeta = utilityBetas.peek();
                    heuristicBeta = heuristicBetas.peek();
                }
                else {
                    utilityAlpha = utilityAlphas.peek();
                    heuristicAlpha = heuristicAlphas.peek();
                    utilityBeta = Math.min(utilityBetas.peek(), ((AbGameNode)tree.getNode()).getUtility());
                    heuristicBeta = Math.min(heuristicBetas.peek(), ((AbGameNode)tree.getNode()).getUtility());
                }
                stack.pop();
                if (lastParent == tree) {
                    utilityAlphas.pop();
                    utilityBetas.pop();
                    heuristicAlphas.pop();
                    heuristicBetas.pop();
                }
                lastParent = (Tree<AbGameNode<A>>)tree.getParent();
            }
            else if ((utilityAlpha < utilityBeta && heuristicAlpha < heuristicBeta) || (tree.getParent() != null && ((AbGameNode)tree.getParent().getNode()).getGame().getCurrentPlayer() < 0)) {
                this.pushChildrenOntoStack(tree, stack);
                utilityAlphas.push(utilityAlpha);
                heuristicAlphas.push(heuristicAlpha);
                utilityBetas.push(utilityBeta);
                heuristicBetas.push(heuristicBeta);
            }
            else {
                if (tree.getParent() == null || ((AbGameNode)tree.getParent().getNode()).getGame().getCurrentPlayer() < 0) {
                    continue;
                }
                if (tree.isRoot() || ((AbGameNode)tree.getParent().getNode()).getGame().getCurrentPlayer() == this.playerId) {
                    ++this.betaCutOffs;
                }
                else {
                    ++this.alphaCutOffs;
                }
                ((AbGameNode)tree.getNode()).setEvaluated(false);
                tree.dropChildren();
                stack.pop();
            }
        }
    }
    
    private double simulationTimeFactor() {
        return 21.9815 * Math.log(1.57606 * TimeUnit.NANOSECONDS.toSeconds(this.nanosLeft()));
    }
    
    private double branchingFactor() {
        return 11.9581 * Math.exp(-0.0447066 * this.averageBranching);
    }
    
    private double timeFactor() {
        return 0.360674 * Math.log(0.4 * TimeUnit.NANOSECONDS.toSeconds(this.nanosLeft()));
    }
    
    private double excessTimeBonus() {
        return 1.11622 * Math.log(1.22474 * this.excessTime);
    }
    
    private int determineDepth() {
        this.depth = (int)Math.max(Math.round(this.branchingFactor() * this.timeFactor()), 2L);
        this.depth = Math.max(this.lastDepth + (int)Math.round(this.excessTimeBonus()), this.depth);
        this.depth = Math.min(this.depth, this.maxDepth);
        this.lastDepth = this.depth;
        return Math.min(this.depth, this.maxDepth);
    }
    
    private boolean sortPromisingCandidates(Tree<AbGameNode<A>> tree, final Comparator<AbGameNode<A>> comparator) {
        for (boolean isDetermined = true; !tree.isLeaf() && ((AbGameNode)tree.getNode()).isEvaluated() && isDetermined; tree = (Tree<AbGameNode<A>>)tree.getChild(0)) {
            isDetermined = (isDetermined && tree.getChildren().stream().allMatch(c -> ((AbGameNode)c.getNode()).getGame().getCurrentPlayer() >= 0));
            if (((AbGameNode)tree.getNode()).getGame().getCurrentPlayer() == this.playerId) {
                tree.sort((Comparator)this.gameAbNodeEvaluatedComparator.reversed().thenComparing(comparator));
            }
            else {
                tree.sort((Comparator)this.gameAbNodeEvaluatedComparator.reversed().thenComparing(comparator.reversed()));
            }
        }
        return ((AbGameNode)tree.getNode()).isEvaluated() && ((AbGameNode)tree.getNode()).getGame().isGameOver();
    }
    
    public String toString() {
        if (this.instanceNr > 1 || AlphaBetaAgent.INSTANCE_NR_COUNTER > 2) {
            return String.format("%s%d", "AlphaBetaAgent#", this.instanceNr);
        }
        return "AlphaBetaAgent";
    }
    
    static {
        AlphaBetaAgent.INSTANCE_NR_COUNTER = 1;
    }
}
