package com.virtuslab.gitmachete.frontend.graph.impl.repository;

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
import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.coloring.SyncToParentStatusToGraphItemColorMapper;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IBranchGetCommitsStrategy;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.impl.items.BranchItem;
import com.virtuslab.gitmachete.frontend.graph.impl.items.CommitItem;

@Accessors(fluent = true)
public class RepositoryGraphBuilder {

  @Setter
  private IGitMacheteRepository repository = NullRepository.getInstance();

  @Setter
  private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static final IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = BaseGitMacheteNonRootBranch::getCommits;
  public static final IBranchGetCommitsStrategy EMPTY_GET_COMMITS = __ -> List.empty();

  public IRepositoryGraph build() {
    Tuple2<List<IGraphItem>, List<List<Integer>>> graphData = deriveGraphItemsAndPositionsOfVisibleEdges();
    return new RepositoryGraph(graphData._1(), graphData._2());
  }

  private Tuple2<List<IGraphItem>, List<List<Integer>>> deriveGraphItemsAndPositionsOfVisibleEdges() {
    List<BaseGitMacheteRootBranch> rootBranches = repository.getRootBranches();

    java.util.List<IGraphItem> graphItems = new ArrayList<>();
    java.util.List<java.util.List<Integer>> positionsOfVisibleEdges = new ArrayList<>();

    for (BaseGitMacheteRootBranch branch : rootBranches) {
      int currentBranchIndex = graphItems.size();
      positionsOfVisibleEdges.add(Collections.emptyList()); // root branches have no visible edges
      addRootBranch(graphItems, branch);
      List<BaseGitMacheteNonRootBranch> downstreamBranches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphItems, positionsOfVisibleEdges, downstreamBranches, currentBranchIndex,
          /* indentLevel */ 0);
    }
    return Tuple.of(List.ofAll(graphItems),
        positionsOfVisibleEdges.stream().map(List::ofAll).collect(List.collector()));
  }

  /**
   * @param graphItems
   *          the collection to store downstream commits and branches
   * @param downstreamBranches
   *          branches to add with their commits
   * @param upstreamBranchIndex
   *          the index of branch which downstream branches (with their commits) are to be added
   */
  private void recursivelyAddCommitsAndBranches(
      java.util.List<IGraphItem> graphItems,
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
        graphItems.get(previousBranchIndex).setNextSiblingItemIndex(graphItems.size());
      }

      int prevSiblingItemIndex = graphItems.size() - 1;
      // We are building some non root branches here so some root branch item has been added already.
      assert prevSiblingItemIndex >= 0 : "There is no previous sibling node but should be";
      buildCommitsAndNonRootBranch(graphItems, branch, prevSiblingItemIndex, indentLevel);

      int upBranchIndex = graphItems.size() - 1;
      List<BaseGitMacheteNonRootBranch> branches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphItems, positionsOfVisibleEdges, /* downstream */ branches,
          upBranchIndex, indentLevel + 1);

      while (positionsOfVisibleEdges.size() < graphItems.size()) {
        positionsOfVisibleEdges.add(new SmartList<>());
      }
      if (!branch.equals(lastDownstreamBranch)) {
        for (int i = upBranchIndex + 1; i < graphItems.size(); ++i) {
          positionsOfVisibleEdges.get(i).add(indentLevel);
        }
      }

      previousBranchIndex = upBranchIndex;
      isFirstBranch = false;
    }
  }

  private void addRootBranch(java.util.List<IGraphItem> graphItems, BaseGitMacheteRootBranch branch) {
    BranchItem branchItem = createBranchItemFor(branch, /* prevSiblingItemIndex */ -1,
        GraphItemColor.GREEN, /* indentLevel */ 0);
    graphItems.add(branchItem);
  }

  private void buildCommitsAndNonRootBranch(
      java.util.List<IGraphItem> graphItems,
      BaseGitMacheteNonRootBranch branch,
      @NonNegative int upstreamBranchIndex,
      @NonNegative int indentLevel) {
    List<IGitMacheteCommit> commits = branchGetCommitsStrategy.getCommitsOf(branch).reverse();

    var syncToParentStatus = branch.getSyncToParentStatus();
    GraphItemColor graphItemColor = SyncToParentStatusToGraphItemColorMapper.getGraphItemColor(syncToParentStatus);
    int branchItemIndex = graphItems.size() + commits.size();
    // We are building some non root branch here so some root branch item has been added already.
    assert branchItemIndex > 0 : "Branch node index is not greater than 0 but should be";

    boolean isFirstItemInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastItemIndex = graphItems.size() - 1;
      // We are building some non root branch here so some root branch item has been added already.
      assert lastItemIndex >= 0 : "Last node index is less than 0 but shouldn't be";
      int prevSiblingItemIndex = isFirstItemInBranch ? upstreamBranchIndex : lastItemIndex;
      int nextSiblingItemIndex = graphItems.size() + 1;
      CommitItem c = new CommitItem(commit, graphItemColor, prevSiblingItemIndex, nextSiblingItemIndex,
          branchItemIndex,
          indentLevel);
      graphItems.add(c);
      isFirstItemInBranch = false;
    }

    int lastItemIndex = graphItems.size() - 1;
    /*
     * If a branch has no commits (possibly due to commits getting strategy being {@code EMPTY_GET_COMMITS}) its {@code
     * prevSiblingItemIndex} is just the {@code upstreamBranchIndex}. Otherwise the {@code prevSiblingItemIndex} is an index of
     * most recently added item (its last commit).
     */
    int prevSiblingItemIndex = commits.isEmpty() ? upstreamBranchIndex : lastItemIndex;

    BranchItem branchItem = createBranchItemFor(branch, prevSiblingItemIndex, graphItemColor, indentLevel);
    graphItems.add(branchItem);
  }

  /**
   * @return {@link BranchItem} for given properties and provide additional attributes if the branch is the current one
   */
  private BranchItem createBranchItemFor(
      BaseGitMacheteBranch branch,
      @GTENegativeOne int prevSiblingItemIndex,
      GraphItemColor graphItemColor,
      @NonNegative int indentLevel) {
    SyncToRemoteStatus syncToRemoteStatus = branch.getSyncToRemoteStatus();
    Option<BaseGitMacheteBranch> currentBranch = repository.getCurrentBranchIfManaged();
    boolean isCurrentBranch = currentBranch.isDefined() && currentBranch.get().equals(branch);
    boolean hasChildItem = !branch.getDownstreamBranches().isEmpty();

    return new BranchItem(branch, graphItemColor, syncToRemoteStatus, prevSiblingItemIndex, indentLevel,
        isCurrentBranch, hasChildItem);
  }
}
