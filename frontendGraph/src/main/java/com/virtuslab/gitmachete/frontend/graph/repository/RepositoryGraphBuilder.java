package com.virtuslab.gitmachete.frontend.graph.repository;

import java.util.ArrayList;
import java.util.Optional;

import io.vavr.collection.List;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;
import com.virtuslab.gitmachete.frontend.graph.coloring.SyncToParentStatusToGraphEdgeColorMapper;
import com.virtuslab.gitmachete.frontend.graph.elements.BranchElement;
import com.virtuslab.gitmachete.frontend.graph.elements.CommitElement;
import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.elements.PhantomElement;
import com.virtuslab.gitmachete.frontend.graph.elements.SplittingElement;

@Accessors(fluent = true)
public class RepositoryGraphBuilder {

  @Setter
  private IGitMacheteRepository repository = NullRepository.getInstance();

  @Setter
  private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static final IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = BaseGitMacheteNonRootBranch::getCommits;
  public static final IBranchGetCommitsStrategy EMPTY_GET_COMMITS = __ -> List.empty();

  public RepositoryGraph build() {
    return new RepositoryGraph(deriveGraphElements());
  }

  private List<IGraphElement> deriveGraphElements() {
    java.util.List<IGraphElement> graphElements = new ArrayList<>();
    List<BaseGitMacheteRootBranch> rootBranches = repository.getRootBranches();
    for (BaseGitMacheteRootBranch branch : rootBranches) {
      int currentBranchIndex = graphElements.size();
      addRootBranch(graphElements, branch);
      List<BaseGitMacheteNonRootBranch> downstreamBranches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphElements, downstreamBranches, currentBranchIndex);
    }
    return List.ofAll(graphElements);
  }

  /**
   * @param graphElements
   *          the collection to store downstream commits and branches
   * @param downstreamBranches
   *          branches to add with their commits
   * @param branchIndex
   *          the index of branch which downstream branches (with their commits) are to be added
   */
  private void recursivelyAddCommitsAndBranches(
      java.util.List<IGraphElement> graphElements,
      List<BaseGitMacheteNonRootBranch> downstreamBranches,
      int branchIndex) {
    int upElementIndex = branchIndex;
    for (BaseGitMacheteNonRootBranch branch : downstreamBranches) {
      SyncToParentStatus syncToParentStatus = branch.getSyncToParentStatus();
      addSplittingGraphElement(graphElements, upElementIndex, syncToParentStatus);

      int splittingElementIndex = graphElements.size() - 1;
      buildCommitsAndNonRootBranch(graphElements, branch, /* upElementIndex */ splittingElementIndex,
          syncToParentStatus);

      upElementIndex = graphElements.size() - 2;
      int upstreamBranchIndex = graphElements.size() - 1;
      List<BaseGitMacheteNonRootBranch> branches = branch.getDownstreamBranches();
      recursivelyAddCommitsAndBranches(graphElements, /* downstream */ branches, upstreamBranchIndex);
    }

    addPhantomGraphElementIfNeeded(graphElements, branchIndex, upElementIndex);
  }

  private void addRootBranch(
      java.util.List<IGraphElement> graphElements,
      BaseGitMacheteRootBranch branch) {
    BranchElement element = createBranchElementFor(branch, /* upElementIndex */ -1, GraphEdgeColor.GREEN,
        branch.getSyncToOriginStatus());
    graphElements.add(element);
  }

  private void buildCommitsAndNonRootBranch(
      java.util.List<IGraphElement> graphElements,
      BaseGitMacheteNonRootBranch branch,
      int upstreamBranchIndex,
      SyncToParentStatus syncToParentStatus) {
    List<IGitMacheteCommit> commits = branchGetCommitsStrategy.getCommitsOf(branch).reverse();

    GraphEdgeColor graphEdgeColor = SyncToParentStatusToGraphEdgeColorMapper.getGraphEdgeColor(syncToParentStatus);
    SyncToOriginStatus syncToOriginStatus = branch.getSyncToOriginStatus();
    int branchElementIndex = graphElements.size() + commits.size();

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastElementIndex = graphElements.size() - 1;
      int upElementIndex = isFirstNodeInBranch ? upstreamBranchIndex : lastElementIndex;
      int downElementIndex = graphElements.size() + 1;
      CommitElement c = new CommitElement(commit, graphEdgeColor, upElementIndex, downElementIndex, branchElementIndex);
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

    BranchElement element = createBranchElementFor(branch, upElementIndex, graphEdgeColor, syncToOriginStatus);
    graphElements.add(element);
  }

  /**
   * @param graphElements
   *          the collection to store downstream commits and branches
   * @param upElementIndex
   *          up element index for the splitting element
   * @param syncToParentStatus
   *          sync to parent status of the branch that will be added just after the splitting element
   */
  private void addSplittingGraphElement(
      java.util.List<IGraphElement> graphElements,
      int upElementIndex,
      SyncToParentStatus syncToParentStatus) {
    int downElementIndex = graphElements.size() + 1;
    int splittingElementIndex = graphElements.size();
    SplittingElement splittingElement = new SplittingElement(
        SyncToParentStatusToGraphEdgeColorMapper.getGraphEdgeColor(syncToParentStatus),
        upElementIndex, downElementIndex);
    graphElements.add(splittingElement);
    graphElements.get(upElementIndex).getDownElementIndexes().add(splittingElementIndex);
  }

  /**
   * From the method name "Needed" means that element at {@code branchIndex} has any down elements (its
   * {@code downElementIndexes} is not empty).
   *
   * @param graphElements
   *          the collection to store downstream commits and branches
   * @param branchIndex
   *          index of branch after which phantom element might be needed
   * @param upElementIndex
   *          up element index for the phantom element
   */
  private void addPhantomGraphElementIfNeeded(java.util.List<IGraphElement> graphElements, int branchIndex,
      int upElementIndex) {
    if (!graphElements.get(branchIndex).getDownElementIndexes().isEmpty()) {
      graphElements.add(new PhantomElement(upElementIndex));
      int phantomElementIndex = graphElements.size() - 1;
      graphElements.get(upElementIndex).getDownElementIndexes().add(phantomElementIndex);
    }
  }

  /**
   * @return {@link BranchElement} for given {@code branch} and {@code upstreamBranchIndex} and provide additional
   *         attributes if the branch is the current one.
   */
  private BranchElement createBranchElementFor(
      BaseGitMacheteBranch branch,
      int upstreamBranchIndex,
      GraphEdgeColor graphEdgeColor,
      SyncToOriginStatus syncToOriginStatus) {

    Optional<BaseGitMacheteBranch> currentBranch = repository.getCurrentBranchIfManaged();

    boolean isCurrentBranch = currentBranch.isPresent() && currentBranch.get().equals(branch);

    return new BranchElement(branch, graphEdgeColor, upstreamBranchIndex, syncToOriginStatus, isCurrentBranch);
  }
}
