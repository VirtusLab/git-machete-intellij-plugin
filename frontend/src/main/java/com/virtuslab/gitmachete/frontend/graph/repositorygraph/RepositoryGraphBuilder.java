package com.virtuslab.gitmachete.frontend.graph.repositorygraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.graph.GraphEdgeColor;
import com.virtuslab.gitmachete.frontend.graph.SyncToParentStatusToGraphEdgeColorMapper;
import com.virtuslab.gitmachete.frontend.graph.model.BranchElement;
import com.virtuslab.gitmachete.frontend.graph.model.CommitElement;
import com.virtuslab.gitmachete.frontend.graph.model.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.model.PhantomElement;
import com.virtuslab.gitmachete.frontend.graph.model.SplittingElement;

@Accessors(fluent = true)
public class RepositoryGraphBuilder {

  @Setter
  private IGitMacheteRepository repository = NullRepository.getInstance();

  @Setter
  private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static final IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = IGitMacheteBranch::getCommits;
  public static final IBranchGetCommitsStrategy EMPTY_GET_COMMITS = b -> io.vavr.collection.List.empty();

  public RepositoryGraph build() {
    return new RepositoryGraph(deriveGraphElements());
  }

  private List<IGraphElement> deriveGraphElements() {
    List<IGraphElement> graphElements = new ArrayList<>();
    List<IGitMacheteBranch> rootBranches = repository.getRootBranches().asJava();
    for (IGitMacheteBranch branch : rootBranches) {
      int currentBranchIndex = graphElements.size();
      SyncToParentStatus syncToParentStatus = branch.getSyncToParentStatus();
      addCommitsWithBranch(graphElements, branch, /* upstreamBranchIndex */ -1, syncToParentStatus);
      List<IGitMacheteBranch> downstreamBranches = branch.getDownstreamBranches().asJava();
      addDownstreamCommitsAndBranches(graphElements, downstreamBranches, currentBranchIndex);
    }
    return graphElements;
  }

  /**
   * @param graphElements
   *          the collection to store downstream commits and branches
   * @param downstreamBranches
   *          branches to add with their commits
   * @param branchIndex
   *          the index of branch which downstream branches (with their commits) are to be added
   */
  private void addDownstreamCommitsAndBranches(
      List<IGraphElement> graphElements,
      List<IGitMacheteBranch> downstreamBranches,
      int branchIndex) {
    int upElementIndex = branchIndex;
    for (IGitMacheteBranch branch : downstreamBranches) {
      SyncToParentStatus syncToParentStatus = branch.getSyncToParentStatus();
      addSplittingGraphElement(graphElements, upElementIndex, syncToParentStatus);

      int splittingElementIndex = graphElements.size() - 1;
      addCommitsWithBranch(graphElements, branch, /* upElementIndex */ splittingElementIndex, syncToParentStatus);

      upElementIndex = graphElements.size() - 2;
      int upstreamBranchIndex = graphElements.size() - 1;
      List<IGitMacheteBranch> branches = branch.getDownstreamBranches().asJava();
      addDownstreamCommitsAndBranches(graphElements, /* downstream */ branches, upstreamBranchIndex);
    }

    addPhantomGraphElementIfNeeded(graphElements, branchIndex, upElementIndex);
  }

  private void addCommitsWithBranch(
      List<IGraphElement> graphElements,
      IGitMacheteBranch branch,
      int upstreamBranchIndex,
      SyncToParentStatus syncToParentStatus) {
    List<IGitMacheteCommit> commits = branchGetCommitsStrategy.getCommitsOf(branch).reverse().asJava();

    GraphEdgeColor graphEdgeColor = SyncToParentStatusToGraphEdgeColorMapper.getGraphEdgeColor(syncToParentStatus);
    SyncToOriginStatus syncToOriginStatus = branch.getSyncToOriginStatus();
    int branchElementIndex = graphElements.size() + commits.size();

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastElementIndex = graphElements.size() - 1;
      int upElementIndex = isFirstNodeInBranch ? upstreamBranchIndex : lastElementIndex;
      int downElementIndex = graphElements.size() + 1;
      CommitElement c = new CommitElement(commit, upElementIndex, downElementIndex, branchElementIndex, graphEdgeColor);
      graphElements.add(c);
      isFirstNodeInBranch = false;
    }

    int lastElementIndex = graphElements.size() - 1;
    /*
     * If a branch has no commits (due to commits getting strategy or because it's a root branch) its {@code
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
      List<IGraphElement> graphElements,
      int upElementIndex,
      SyncToParentStatus syncToParentStatus) {
    int downElementIndex = graphElements.size() + 1;
    int splittingElementIndex = graphElements.size();
    SplittingElement splittingElement = new SplittingElement(upElementIndex, downElementIndex,
        SyncToParentStatusToGraphEdgeColorMapper.getGraphEdgeColor(syncToParentStatus));
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
  private void addPhantomGraphElementIfNeeded(List<IGraphElement> graphElements, int branchIndex, int upElementIndex) {
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
      IGitMacheteBranch branch,
      int upstreamBranchIndex,
      GraphEdgeColor graphEdgeColor,
      SyncToOriginStatus syncToOriginStatus) {

    Optional<@Nullable IGitMacheteBranch> currentBranch = repository.getCurrentBranchIfManaged();

    boolean isCurrentBranch = currentBranch.isPresent() && currentBranch.get().equals(branch);

    return new BranchElement(branch, upstreamBranchIndex, graphEdgeColor, syncToOriginStatus, isCurrentBranch);
  }
}
