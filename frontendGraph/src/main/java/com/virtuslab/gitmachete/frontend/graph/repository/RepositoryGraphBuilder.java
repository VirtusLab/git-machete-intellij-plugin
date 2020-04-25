package com.virtuslab.gitmachete.frontend.graph.repository;

import java.util.ArrayList;
import java.util.Collections;

import com.intellij.util.SmartList;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;
import com.virtuslab.gitmachete.frontend.graph.coloring.SyncToParentStatusToGraphEdgeColorMapper;
import com.virtuslab.gitmachete.frontend.graph.nodes.BranchNode;
import com.virtuslab.gitmachete.frontend.graph.nodes.CommitNode;
import com.virtuslab.gitmachete.frontend.graph.nodes.IGraphNode;

@Accessors(fluent = true)
public class RepositoryGraphBuilder {

  @Setter
  private IGitMacheteRepository repository = NullRepository.getInstance();

  @Setter
  private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static final IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = BaseGitMacheteNonRootBranch::getCommits;
  public static final IBranchGetCommitsStrategy EMPTY_GET_COMMITS = __ -> List.empty();

  public RepositoryGraph build() {
    Tuple2<List<IGraphNode>, List<List<Integer>>> graphData = deriveGraphNodesAndPositionsOfVisibleEdges();
    return new RepositoryGraph(graphData._1(), graphData._2());
  }

  private Tuple2<List<IGraphNode>, List<List<Integer>>> deriveGraphNodesAndPositionsOfVisibleEdges() {
    List<BaseGitMacheteRootBranch> rootBranches = repository.getRootBranches();

    java.util.List<IGraphNode> graphNodes = new ArrayList<>();
    java.util.List<java.util.List<Integer>> positionsOfVisibleEdges = new ArrayList<>();

    for (BaseGitMacheteRootBranch branch : rootBranches) {
      int currentBranchIndex = graphNodes.size();
      positionsOfVisibleEdges.add(Collections.emptyList()); // root branches have no visible edges
      addRootBranch(graphNodes, branch);
      List<BaseGitMacheteNonRootBranch> downstreamBranches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphNodes, positionsOfVisibleEdges, downstreamBranches, currentBranchIndex,
          /* indentLevel */ 0);
    }
    return Tuple.of(List.ofAll(graphNodes),
        positionsOfVisibleEdges.stream().map(List::ofAll).collect(List.collector()));
  }

  /**
   * @param graphNodes
   *          the collection to store downstream commits and branches
   * @param downstreamBranches
   *          branches to add with their commits
   * @param upstreamBranchIndex
   *          the index of branch which downstream branches (with their commits) are to be added
   */
  private void recursivelyAddCommitsAndBranches(
      java.util.List<IGraphNode> graphNodes,
      java.util.List<java.util.List<Integer>> positionsOfVisibleEdges,
      List<BaseGitMacheteNonRootBranch> downstreamBranches,
      @GTENegativeOne int upstreamBranchIndex,
      @NonNegative int indentLevel) {
    boolean isFirstBranch = true;
    var lastDownstreamBranch = downstreamBranches.size() > 0
        ? downstreamBranches.get(downstreamBranches.size() - 1)
        : null;

    int previousBranchIndex = upstreamBranchIndex;
    for (BaseGitMacheteNonRootBranch branch : downstreamBranches) {
      if (!isFirstBranch) {
        graphNodes.get(previousBranchIndex).setNextSiblingNodeIndex(graphNodes.size());
      }

      int prevSiblingNodeIndex = graphNodes.size() - 1;
      // We are building some non root branches here so some root branch node has been added already.
      assert prevSiblingNodeIndex >= 0 : "There are no previous sibling node but should be";
      buildCommitsAndNonRootBranch(graphNodes, branch, prevSiblingNodeIndex, indentLevel);

      int upBranchIndex = graphNodes.size() - 1;
      List<BaseGitMacheteNonRootBranch> branches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphNodes, positionsOfVisibleEdges, /* downstream */ branches,
          upBranchIndex, indentLevel + 1);

      while (positionsOfVisibleEdges.size() < graphNodes.size()) {
        positionsOfVisibleEdges.add(new SmartList<>());
      }
      if (!branch.equals(lastDownstreamBranch)) {
        for (int i = upBranchIndex + 1; i < graphNodes.size(); ++i) {
          positionsOfVisibleEdges.get(i).add(indentLevel);
        }
      }

      previousBranchIndex = upBranchIndex;
      isFirstBranch = false;
    }
  }

  private void addRootBranch(java.util.List<IGraphNode> graphNodes, BaseGitMacheteRootBranch branch) {
    BranchNode branchNode = createBranchNodeFor(branch, /* prevSiblingNodeIndex */ -1,
        GraphEdgeColor.GREEN, /* indentLevel */ 0);
    graphNodes.add(branchNode);
  }

  private void buildCommitsAndNonRootBranch(
      java.util.List<IGraphNode> graphNodes,
      BaseGitMacheteNonRootBranch branch,
      @NonNegative int upstreamBranchIndex,
      @NonNegative int indentLevel) {
    List<IGitMacheteCommit> commits = branchGetCommitsStrategy.getCommitsOf(branch).reverse();

    var syncToParentStatus = branch.getSyncToParentStatus();
    GraphEdgeColor graphEdgeColor = SyncToParentStatusToGraphEdgeColorMapper.getGraphEdgeColor(syncToParentStatus);
    int branchNodeIndex = graphNodes.size() + commits.size();
    // We are building some non root branch here so some root branch node has been added already.
    assert branchNodeIndex > 0 : "Branch node index is not greater than 0 but should be";

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastNodeIndex = graphNodes.size() - 1;
      // We are building some non root branch here so some root branch node has been added already.
      assert lastNodeIndex >= 0 : "Last node index is less than 0 but shouldn't be";
      int prevSiblingNodeIndex = isFirstNodeInBranch ? upstreamBranchIndex : lastNodeIndex;
      int nextSiblingNodeIndex = graphNodes.size() + 1;
      CommitNode c = new CommitNode(commit, graphEdgeColor, prevSiblingNodeIndex, nextSiblingNodeIndex,
          branchNodeIndex,
          indentLevel);
      graphNodes.add(c);
      isFirstNodeInBranch = false;
    }

    int lastNodeIndex = graphNodes.size() - 1;
    /*
     * If a branch has no commits (possibly due to commits getting strategy being {@code EMPTY_GET_COMMITS}) its {@code
     * prevSiblingNodeIndex} is just the {@code upstreamBranchIndex}. Otherwise the {@code prevSiblingNodeIndex} is an index of
     * most recently added node (its last commit).
     */
    int prevSiblingNodeIndex = commits.isEmpty() ? upstreamBranchIndex : lastNodeIndex;

    BranchNode branchNode = createBranchNodeFor(branch, prevSiblingNodeIndex, graphEdgeColor, indentLevel);
    graphNodes.add(branchNode);
  }

  /**
   * @return {@link BranchNode} for given properties and provide additional
   *         attributes if the branch is the current one.
   */
  private BranchNode createBranchNodeFor(
      BaseGitMacheteBranch branch,
      @GTENegativeOne int prevSiblingNodeIndex,
      GraphEdgeColor graphEdgeColor,
      @NonNegative int indentLevel) {
    SyncToRemoteStatus syncToRemoteStatus = branch.getSyncToRemoteStatus();

    Option<BaseGitMacheteBranch> currentBranch = repository.getCurrentBranchIfManaged();
    boolean isCurrentBranch = currentBranch.isDefined() && currentBranch.get().equals(branch);

    boolean hasChildNode = !branch.getDownstreamBranches().isEmpty();
    return new BranchNode(branch, graphEdgeColor, syncToRemoteStatus, prevSiblingNodeIndex, indentLevel,
        isCurrentBranch, hasChildNode);
  }
}
