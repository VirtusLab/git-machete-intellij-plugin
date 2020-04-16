package com.virtuslab.gitmachete.frontend.graph.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import com.intellij.util.SmartList;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;
import com.virtuslab.gitmachete.frontend.graph.coloring.SyncToParentStatusToGraphEdgeColorMapper;
import com.virtuslab.gitmachete.frontend.graph.elements.BranchElement;
import com.virtuslab.gitmachete.frontend.graph.elements.CommitElement;
import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;

@Accessors(fluent = true)
public class RepositoryGraphBuilder {

  @Setter
  private IGitMacheteRepository repository = NullRepository.getInstance();

  @Setter
  private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static final IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = BaseGitMacheteNonRootBranch::getCommits;
  public static final IBranchGetCommitsStrategy EMPTY_GET_COMMITS = __ -> List.empty();

  public RepositoryGraph build() {
    Tuple2<List<IGraphElement>, List<List<Integer>>> graphData = deriveGraphElementsAndPositionsOfVisibleEdges();
    return new RepositoryGraph(graphData._1(), graphData._2());
  }

  private Tuple2<List<IGraphElement>, List<List<Integer>>> deriveGraphElementsAndPositionsOfVisibleEdges() {
    List<BaseGitMacheteRootBranch> rootBranches = repository.getRootBranches();

    java.util.List<IGraphElement> graphElements = new ArrayList<>();
    java.util.List<java.util.List<Integer>> positionsOfVisibleEdges = new ArrayList<>(
        Collections.nCopies(rootBranches.size(), new SmartList<>()));

    for (BaseGitMacheteRootBranch branch : rootBranches) {
      int currentBranchIndex = graphElements.size();
      addRootBranch(graphElements, branch);
      List<BaseGitMacheteNonRootBranch> downstreamBranches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphElements, positionsOfVisibleEdges, downstreamBranches, currentBranchIndex,
          /* indentLevel */ 0);
    }
    return Tuple.of(List.ofAll(graphElements),
        positionsOfVisibleEdges.stream().map(List::ofAll).collect(List.collector()));
  }

  /**
   * @param graphElements
   *          the collection to store downstream commits and branches
   * @param downstreamBranches
   *          branches to add with their commits
   * @param upstreamBranchIndex
   *          the index of branch which downstream branches (with their commits) are to be added
   */
  private void recursivelyAddCommitsAndBranches(
      java.util.List<IGraphElement> graphElements,
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
        graphElements.get(previousBranchIndex).setDownElementIndex(graphElements.size());
      }

      int upElementIndex = graphElements.size() - 1;
      assert upElementIndex >= 0;
      buildCommitsAndNonRootBranch(graphElements, branch, upElementIndex, indentLevel);

      int upBranchIndex = graphElements.size() - 1;
      List<BaseGitMacheteNonRootBranch> branches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphElements, positionsOfVisibleEdges, /* downstream */ branches,
          upBranchIndex, indentLevel + 1);

      while (positionsOfVisibleEdges.size() < graphElements.size()) {
        positionsOfVisibleEdges.add(new SmartList<>());
      }
      if (!branch.equals(lastDownstreamBranch)) {
        for (int i = upBranchIndex + 1; i < graphElements.size(); ++i) {
          positionsOfVisibleEdges.get(i).add(indentLevel);
        }
      }

      previousBranchIndex = upBranchIndex;
      isFirstBranch = false;
    }
  }

  private void addRootBranch(java.util.List<IGraphElement> graphElements, BaseGitMacheteRootBranch branch) {
    BranchElement element = createBranchElementFor(branch, /* upElementIndex */ -1,
        GraphEdgeColor.GREEN, /* indentLevel */ 0);
    graphElements.add(element);
  }

  private void buildCommitsAndNonRootBranch(
      java.util.List<IGraphElement> graphElements,
      BaseGitMacheteNonRootBranch branch,
      @NonNegative int upstreamBranchIndex,
      @NonNegative int indentLevel) {
    List<IGitMacheteCommit> commits = branchGetCommitsStrategy.getCommitsOf(branch).reverse();

    var syncToParentStatus = branch.getSyncToParentStatus();
    GraphEdgeColor graphEdgeColor = SyncToParentStatusToGraphEdgeColorMapper.getGraphEdgeColor(syncToParentStatus);
    int branchElementIndex = graphElements.size() + commits.size();
    assert branchElementIndex > 0;

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastElementIndex = graphElements.size() - 1;
      assert lastElementIndex >= 0;
      int upElementIndex = isFirstNodeInBranch ? upstreamBranchIndex : lastElementIndex;
      int downElementIndex = graphElements.size() + 1;
      CommitElement c = new CommitElement(commit, graphEdgeColor, upElementIndex, downElementIndex, branchElementIndex,
          indentLevel);
      graphElements.add(c);
      isFirstNodeInBranch = false;
    }

    int lastElementIndex = graphElements.size() - 1;
    /*
     * If a branch has no commits (possibly due to commits getting strategy being {@code EMPTY_GET_COMMITS}) its {@code
     * upElementIndex} is just the {@code upstreamBranchIndex}. Otherwise the {@code upElementIndex} is an index of most
     * recently added element (its last commit).
     */
    int upElementIndex = commits.isEmpty() ? upstreamBranchIndex : lastElementIndex;

    BranchElement element = createBranchElementFor(branch, upElementIndex, graphEdgeColor, indentLevel);
    graphElements.add(element);
  }

  /**
   * @return {@link BranchElement} for given {@code branch} and {@code upstreamBranchIndex} and provide additional
   *         attributes if the branch is the current one.
   */
  private BranchElement createBranchElementFor(
      BaseGitMacheteBranch branch,
      @GTENegativeOne int upstreamBranchIndex,
      GraphEdgeColor graphEdgeColor,
      @NonNegative int indentLevel) {
    ISyncToRemoteStatus syncToRemoteStatus = branch.getSyncToRemoteStatus();

    Optional<BaseGitMacheteBranch> currentBranch = repository.getCurrentBranchIfManaged();
    boolean isCurrentBranch = currentBranch.isPresent() && currentBranch.get().equals(branch);

    boolean hasSubelement = !branch.getDownstreamBranches().isEmpty();
    return new BranchElement(branch, graphEdgeColor, upstreamBranchIndex, syncToRemoteStatus, isCurrentBranch,
        indentLevel, hasSubelement);
  }
}
