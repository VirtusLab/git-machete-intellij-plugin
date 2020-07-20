package com.virtuslab.gitmachete.frontend.graph.impl.repository;

import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSync;
import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.MergedToParent;
import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.OutOfSync;
import static com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor.GRAY;
import static com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor.GREEN;
import static com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor.RED;
import static com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor.TRANSPARENT;
import static com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor.YELLOW;

import java.util.ArrayList;
import java.util.Collections;

import com.intellij.util.SmartList;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.NullGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IBranchGetCommitsStrategy;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.impl.items.BranchItem;
import com.virtuslab.gitmachete.frontend.graph.impl.items.CommitItem;

@Accessors(fluent = true)
@CustomLog
public class RepositoryGraphBuilder {

  @Setter
  private IGitMacheteRepositorySnapshot repository = NullGitMacheteRepositorySnapshot.getInstance();

  @Setter
  private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static final IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = IGitMacheteNonRootBranch::getCommits;
  public static final IBranchGetCommitsStrategy EMPTY_GET_COMMITS = __ -> List.empty();

  public IRepositoryGraph build() {
    LOG.startTimer().debug("Entering");

    Tuple2<List<IGraphItem>, List<List<Integer>>> graphData = deriveGraphItemsAndPositionsOfVisibleEdges();
    var result = new RepositoryGraph(graphData._1(), graphData._2());

    LOG.withTimeElapsed().debug("Finished");
    return result;
  }

  private Tuple2<List<IGraphItem>, List<List<Integer>>> deriveGraphItemsAndPositionsOfVisibleEdges() {
    List<IGitMacheteRootBranch> rootBranches = repository.getRootBranches();

    java.util.List<IGraphItem> graphItems = new ArrayList<>();
    java.util.List<java.util.List<Integer>> positionsOfVisibleEdges = new ArrayList<>();

    for (IGitMacheteRootBranch branch : rootBranches) {
      int currentBranchIndex = graphItems.size();
      positionsOfVisibleEdges.add(Collections.emptyList()); // root branches have no visible edges
      addRootBranch(graphItems, branch);
      List<? extends IGitMacheteNonRootBranch> childBranches = branch.getChildBranches();
      recursivelyAddCommitsAndBranches(graphItems, positionsOfVisibleEdges, childBranches, currentBranchIndex,
          /* indentLevel */ 0);
    }
    return Tuple.of(List.ofAll(graphItems),
        positionsOfVisibleEdges.stream().map(List::ofAll).collect(List.collector()));
  }

  /**
   * @param graphItems
   *          the collection to store child commits and branches
   * @param childBranches
   *          branches to add with their commits
   * @param parentBranchIndex
   *          the index of branch which child branches (with their commits) are to be added
   */
  private void recursivelyAddCommitsAndBranches(
      java.util.List<IGraphItem> graphItems,
      java.util.List<java.util.List<Integer>> positionsOfVisibleEdges,
      List<? extends IGitMacheteNonRootBranch> childBranches,
      @GTENegativeOne int parentBranchIndex,
      @NonNegative int indentLevel) {
    boolean isFirstBranch = true;
    var lastChildBranch = childBranches.size() > 0
        ? childBranches.get(childBranches.size() - 1)
        : null;

    int previousBranchIndex = parentBranchIndex;
    for (IGitMacheteNonRootBranch branch : childBranches) {
      if (!isFirstBranch) {
        graphItems.get(previousBranchIndex).setNextSiblingItemIndex(graphItems.size());
      }

      int prevSiblingItemIndex = graphItems.size() - 1;
      // We are building some non root branches here so some root branch item has been added already.
      assert prevSiblingItemIndex >= 0 : "There is no previous sibling node but should be";
      buildCommitsAndNonRootBranch(graphItems, branch, prevSiblingItemIndex, indentLevel);

      int upBranchIndex = graphItems.size() - 1;
      List<? extends IGitMacheteNonRootBranch> branches = branch.getChildBranches();
      recursivelyAddCommitsAndBranches(graphItems, positionsOfVisibleEdges, /* child */ branches,
          upBranchIndex, indentLevel + 1);

      while (positionsOfVisibleEdges.size() < graphItems.size()) {
        positionsOfVisibleEdges.add(new SmartList<>());
      }
      if (!branch.equals(lastChildBranch)) {
        for (int i = upBranchIndex + 1; i < graphItems.size(); ++i) {
          positionsOfVisibleEdges.get(i).add(indentLevel);
        }
      }

      previousBranchIndex = upBranchIndex;
      isFirstBranch = false;
    }
  }

  private void addRootBranch(java.util.List<IGraphItem> graphItems, IGitMacheteRootBranch branch) {
    BranchItem branchItem = createBranchItemFor(branch, /* prevSiblingItemIndex */ -1,
        GraphItemColor.GREEN, /* indentLevel */ 0);
    graphItems.add(branchItem);
  }

  private static final Map<SyncToParentStatus, GraphItemColor> ITEM_COLORS = HashMap.of(
      MergedToParent, GRAY,
      InSyncButForkPointOff, YELLOW,
      OutOfSync, RED,
      InSync, GREEN);

  private static GraphItemColor getGraphItemColor(SyncToParentStatus syncToParentStatus) {
    return ITEM_COLORS.getOrElse(syncToParentStatus, TRANSPARENT);
  }

  private void buildCommitsAndNonRootBranch(
      java.util.List<IGraphItem> graphItems,
      IGitMacheteNonRootBranch branch,
      @NonNegative int parentBranchIndex,
      @NonNegative int indentLevel) {
    List<IGitMacheteCommit> commits = branchGetCommitsStrategy.getCommitsOf(branch).reverse();

    var syncToParentStatus = branch.getSyncToParentStatus();
    GraphItemColor graphItemColor = getGraphItemColor(syncToParentStatus);
    int branchItemIndex = graphItems.size() + commits.size();
    // We are building some non root branch here so some root branch item has been added already.
    assert branchItemIndex > 0 : "Branch node index is not greater than 0 but should be";

    boolean isFirstItemInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastItemIndex = graphItems.size() - 1;
      // We are building some non root branch here so some root branch item has been added already.
      assert lastItemIndex >= 0 : "Last node index is less than 0 but shouldn't be";
      int prevSiblingItemIndex = isFirstItemInBranch ? parentBranchIndex : lastItemIndex;
      int nextSiblingItemIndex = graphItems.size() + 1;
      CommitItem c = new CommitItem(commit, branch, graphItemColor, prevSiblingItemIndex, nextSiblingItemIndex, indentLevel);
      graphItems.add(c);
      isFirstItemInBranch = false;
    }

    int lastItemIndex = graphItems.size() - 1;
    /*
     * If a branch has no commits (possibly due to commits getting strategy being {@code EMPTY_GET_COMMITS}) its {@code
     * prevSiblingItemIndex} is just the {@code parentBranchIndex}. Otherwise the {@code prevSiblingItemIndex} is an index of
     * most recently added item (its last commit).
     */
    int prevSiblingItemIndex = commits.isEmpty() ? parentBranchIndex : lastItemIndex;

    BranchItem branchItem = createBranchItemFor(branch, prevSiblingItemIndex, graphItemColor, indentLevel);
    graphItems.add(branchItem);
  }

  /**
   * @return {@link BranchItem} for given properties and provide additional attributes if the branch is the current one
   */
  private BranchItem createBranchItemFor(
      IGitMacheteBranch branch,
      @GTENegativeOne int prevSiblingItemIndex,
      GraphItemColor graphItemColor,
      @NonNegative int indentLevel) {
    SyncToRemoteStatus syncToRemoteStatus = branch.getSyncToRemoteStatus();
    Option<IGitMacheteBranch> currentBranch = repository.getCurrentBranchIfManaged();
    boolean isCurrentBranch = currentBranch.isDefined() && currentBranch.get().equals(branch);
    boolean hasChildItem = !branch.getChildBranches().isEmpty();

    return new BranchItem(branch, graphItemColor, syncToRemoteStatus, prevSiblingItemIndex, indentLevel,
        isCurrentBranch, hasChildItem);
  }
}
