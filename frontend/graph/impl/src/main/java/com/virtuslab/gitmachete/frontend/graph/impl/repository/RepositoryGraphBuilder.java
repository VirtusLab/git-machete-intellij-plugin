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
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.NullGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IBranchGetCommitsStrategy;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.impl.items.BranchItem;
import com.virtuslab.gitmachete.frontend.graph.impl.items.CommitItem;

@Accessors(fluent = true)
public class RepositoryGraphBuilder {

  @Setter
  private IGitMacheteRepositorySnapshot repositorySnapshot = NullGitMacheteRepositorySnapshot.getInstance();

  @Setter
  private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static final IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = INonRootManagedBranchSnapshot::getUniqueCommits;
  public static final IBranchGetCommitsStrategy EMPTY_GET_COMMITS = __ -> List.empty();

  public IRepositoryGraph build() {
    Tuple2<List<IGraphItem>, List<List<Integer>>> graphData = deriveGraphItemsAndPositionsOfVisibleEdges();
    return new RepositoryGraph(graphData._1(), graphData._2());
  }

  private Tuple2<List<IGraphItem>, List<List<Integer>>> deriveGraphItemsAndPositionsOfVisibleEdges() {
    List<IRootManagedBranchSnapshot> rootBranches = repositorySnapshot.getRootBranches();

    java.util.List<IGraphItem> graphItems = new ArrayList<>();
    java.util.List<java.util.List<Integer>> positionsOfVisibleEdges = new ArrayList<>();

    for (val rootBranch : rootBranches) {
      int currentBranchIndex = graphItems.size();
      positionsOfVisibleEdges.add(Collections.emptyList()); // root branches have no visible edges
      addRootBranch(graphItems, rootBranch);
      List<? extends INonRootManagedBranchSnapshot> childBranches = rootBranch.getChildren();
      recursivelyAddCommitsAndBranches(graphItems, positionsOfVisibleEdges, childBranches, currentBranchIndex,
          /* indentLevel */ 0);
    }
    return Tuple.of(
        List.ofAll(graphItems),
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
      List<? extends INonRootManagedBranchSnapshot> childBranches,
      @GTENegativeOne int parentBranchIndex,
      @NonNegative int indentLevel) {
    boolean isFirstBranch = true;
    @SuppressWarnings("nullness:conditional") val lastChildBranch = childBranches.size() > 0
        ? childBranches.get(childBranches.size() - 1)
        : null;

    int previousBranchIndex = parentBranchIndex;
    for (val nonRootBranch : childBranches) {
      if (!isFirstBranch) {
        graphItems.get(previousBranchIndex).setNextSiblingItemIndex(graphItems.size());
      }

      int prevSiblingItemIndex = graphItems.size() - 1;
      // We are building some non root branches here so some root branch item has been added already.
      assert prevSiblingItemIndex >= 0 : "There is no previous sibling node but should be";
      buildCommitsAndNonRootBranch(graphItems, nonRootBranch, prevSiblingItemIndex, indentLevel);

      int upBranchIndex = graphItems.size() - 1;
      List<? extends INonRootManagedBranchSnapshot> branches = nonRootBranch.getChildren();
      recursivelyAddCommitsAndBranches(graphItems, positionsOfVisibleEdges, /* child */ branches,
          upBranchIndex, indentLevel + 1);

      while (positionsOfVisibleEdges.size() < graphItems.size()) {
        positionsOfVisibleEdges.add(new SmartList<>());
      }
      if (!nonRootBranch.equals(lastChildBranch)) {
        for (int i = upBranchIndex + 1; i < graphItems.size(); ++i) {
          positionsOfVisibleEdges.get(i).add(indentLevel);
        }
      }

      previousBranchIndex = upBranchIndex;
      isFirstBranch = false;
    }
  }

  private void addRootBranch(java.util.List<IGraphItem> graphItems, IRootManagedBranchSnapshot branch) {
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
      INonRootManagedBranchSnapshot branch,
      @NonNegative int parentBranchIndex,
      @NonNegative int indentLevel) {
    List<ICommitOfManagedBranch> commits = branchGetCommitsStrategy.getCommitsOf(branch).reverse();

    val syncToParentStatus = branch.getSyncToParentStatus();
    GraphItemColor graphItemColor = getGraphItemColor(syncToParentStatus);
    int branchItemIndex = graphItems.size() + commits.size();
    // We are building some non root branch here so some root branch item has been added already.
    assert branchItemIndex > 0 : "Branch node index is not greater than 0 but should be";

    boolean isFirstItemInBranch = true;
    for (ICommitOfManagedBranch commit : commits) {
      int lastItemIndex = graphItems.size() - 1;
      // We are building some non root branch here so some root branch item has been added already.
      assert lastItemIndex >= 0 : "Last node index is less than 0 but shouldn't be";
      int prevSiblingItemIndex = isFirstItemInBranch ? parentBranchIndex : lastItemIndex;
      int nextSiblingItemIndex = graphItems.size() + 1;
      val c = new CommitItem(commit, branch, graphItemColor, prevSiblingItemIndex, nextSiblingItemIndex, indentLevel);
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
      IManagedBranchSnapshot branch,
      @GTENegativeOne int prevSiblingItemIndex,
      GraphItemColor graphItemColor,
      @NonNegative int indentLevel) {
    RelationToRemote relationToRemote = branch.getRelationToRemote();
    val currentBranch = repositorySnapshot.getCurrentBranchIfManaged();
    boolean isCurrentBranch = currentBranch != null && currentBranch.equals(branch);
    boolean hasChildItem = !branch.getChildren().isEmpty();

    return new BranchItem(branch, graphItemColor, relationToRemote, prevSiblingItemIndex, indentLevel,
        isCurrentBranch, hasChildItem);
  }
}
