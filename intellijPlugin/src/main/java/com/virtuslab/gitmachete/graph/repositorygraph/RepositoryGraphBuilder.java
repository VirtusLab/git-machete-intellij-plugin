package com.virtuslab.gitmachete.graph.repositorygraph;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import com.virtuslab.gitmachete.graph.model.BranchElement;
import com.virtuslab.gitmachete.graph.model.CommitElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import com.virtuslab.gitmachete.graph.model.PhantomElement;
import com.virtuslab.gitmachete.graph.model.SplittingElement;
import com.virtuslab.gitmachete.graph.repositorygraph.data.NullRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class RepositoryGraphBuilder {
  private static final Logger LOG = Logger.getInstance(RepositoryGraphBuilder.class);

  @Nonnull @Setter private IGitMacheteRepository repository = NullRepository.getInstance();

  @Nonnull @Setter
  private IBranchComputeCommitsStrategy branchComputeCommitsStrategy = DEFAULT_COMPUTE_COMMITS;

  public static IBranchComputeCommitsStrategy DEFAULT_COMPUTE_COMMITS =
      IGitMacheteBranch::computeCommits;
  public static IBranchComputeCommitsStrategy EMPTY_COMPUTE_COMMITS = b -> Collections.emptyList();

  public RepositoryGraph build() {
    List<IGraphElement> elementsOfRepository;
    try {
      elementsOfRepository = computeGraphElements();
    } catch (GitException e) {
      LOG.error("Unable to build elements of repository graph", e);
      elementsOfRepository = Collections.emptyList();
    }
    return new RepositoryGraph(elementsOfRepository);
  }

  @Nonnull
  private List<IGraphElement> computeGraphElements() throws GitException {
    List<IGraphElement> graphElements = new ArrayList<>();
    List<IGitMacheteBranch> rootBranches = repository.getRootBranches();
    for (IGitMacheteBranch branch : rootBranches) {
      int currentBranchIndex = graphElements.size();
      // todo set syncToParentStatus later (?)
      SyncToParentStatus syncToParentStatus = branch.computeSyncToParentStatus();
      addCommitsWithBranch(graphElements, branch, /*upstreamBranchIndex*/ -1, syncToParentStatus);
      List<IGitMacheteBranch> downstreamBranches = branch.getDownstreamBranches();
      addDownstreamCommitsAndBranches(graphElements, downstreamBranches, currentBranchIndex);
    }
    return graphElements;
  }

  /**
   * @param graphElements the collection to store downstream commits and branches
   * @param downstreamBranches branches to add with their commits
   * @param branchIndex the index of branch which downstream branches (with their commits) are to be
   *     added
   */
  private void addDownstreamCommitsAndBranches(
      List<IGraphElement> graphElements,
      List<IGitMacheteBranch> downstreamBranches,
      int branchIndex)
      throws GitException {
    int upElementIndex = branchIndex;
    for (IGitMacheteBranch branch : downstreamBranches) {
      // todo set syncToParentStatus later (?)
      SyncToParentStatus syncToParentStatus = branch.computeSyncToParentStatus();
      addSplittingGraphElement(graphElements, upElementIndex, syncToParentStatus);

      int splittingElementIndex = graphElements.size() - 1;
      addCommitsWithBranch(
          graphElements, branch, /*upElementIndex*/ splittingElementIndex, syncToParentStatus);

      upElementIndex = graphElements.size() - 2;
      int upstreamBranchIndex = graphElements.size() - 1;
      List<IGitMacheteBranch> branches = branch.getDownstreamBranches();
      addDownstreamCommitsAndBranches(graphElements, /*downstream*/ branches, upstreamBranchIndex);
    }

    addPhantomGraphElementIfNeeded(graphElements, branchIndex, upElementIndex);
  }

  private void addCommitsWithBranch(
      List<IGraphElement> graphElements,
      IGitMacheteBranch branch,
      int upstreamBranchIndex,
      SyncToParentStatus syncToParentStatus)
      throws GitException {
    List<IGitMacheteCommit> commits =
        Lists.reverse(branchComputeCommitsStrategy.computeCommitsOf(branch));

    SyncToOriginStatus syncToOriginStatus = branch.computeSyncToOriginStatus();

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastElementIndex = graphElements.size() - 1;
      int upElementIndex = isFirstNodeInBranch ? upstreamBranchIndex : lastElementIndex;
      int downElementIndex = graphElements.size() + 1;
      CommitElement c =
          new CommitElement(
              commit,
              upElementIndex,
              downElementIndex,
              /*containingBranchSyncToParentStatus*/ syncToParentStatus);
      graphElements.add(c);
      isFirstNodeInBranch = false;
    }

    int lastElementIndex = graphElements.size() - 1;
    /*
     * If a branch has no commits (due to commits getting strategy or because its a root branch)
     * its upElementIndex is just the upstreamBranchIndex.
     * Otherwise the upElementIndex is an index of most recently added element (its last commit).
     */
    int upElementIndex = commits.isEmpty() ? upstreamBranchIndex : lastElementIndex;

    BranchElement element =
        createBranchElementFor(branch, upElementIndex, syncToParentStatus, syncToOriginStatus);
    graphElements.add(element);
  }

  /**
   * @param graphElements the collection to store downstream commits and branches
   * @param upElementIndex upElementIndex for the splitting element
   * @param syncToParentStatus syncToParentStatus of the branch that will be added just after the
   *     splitting element
   */
  private void addSplittingGraphElement(
      List<IGraphElement> graphElements,
      int upElementIndex,
      SyncToParentStatus syncToParentStatus) {
    int downElementIndex = graphElements.size() + 1;
    int splittingElementIndex = graphElements.size();
    SplittingElement splittingElement =
        new SplittingElement(upElementIndex, downElementIndex, syncToParentStatus);
    graphElements.add(splittingElement);
    graphElements.get(upElementIndex).getDownElementIndexes().add(splittingElementIndex);
  }

  /**
   * Adds element that purpose is to keep downstream branches nodes in the same column (shifts right
   * the last one). It is not intended to show this element. This is a hackish solution to solve the
   * problem. It should be treated as temporary and an appropriate one shall be implemented (own
   * {@link com.intellij.vcs.log.graph.api.printer.PrintElementGenerator} might be needed).
   *
   * <p>From the method name "Needed" means that element at {@code branchIndex} has any down
   * elements (its {@code downElementIndexes} is not empty).
   *
   * @param graphElements the collection to store downstream commits and branches
   * @param branchIndex index of branch after which phantom element might be needed
   * @param upElementIndex upElementIndex for the phantom element
   */
  private void addPhantomGraphElementIfNeeded(
      List<IGraphElement> graphElements, int branchIndex, int upElementIndex) {
    if (!graphElements.get(branchIndex).getDownElementIndexes().isEmpty()) {
      graphElements.add(new PhantomElement(upElementIndex));
      int phantomElementIndex = graphElements.size() - 1;
      graphElements.get(upElementIndex).getDownElementIndexes().add(phantomElementIndex);
    }
  }

  /**
   * @return {@link BranchElement} for given {@code branch} and {@code upstreamBranchIndex} and
   *     provide additional attributes if the branch is the current one.
   */
  @Nonnull
  private BranchElement createBranchElementFor(
      IGitMacheteBranch branch,
      int upstreamBranchIndex,
      SyncToParentStatus syncToParentStatus,
      SyncToOriginStatus syncToOriginStatus) {

    Optional<IGitMacheteBranch> currentBranch = Optional.empty();
    try {
      currentBranch = repository.getCurrentBranchIfManaged();
    } catch (GitMacheteException e) {
      LOG.error("Unable to get current branch", e);
    }

    boolean isCurrentBranch = currentBranch.isPresent() && currentBranch.get().equals(branch);

    return new BranchElement(
        branch, upstreamBranchIndex, syncToParentStatus, syncToOriginStatus, isCurrentBranch);
  }
}
