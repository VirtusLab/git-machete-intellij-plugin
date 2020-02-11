package com.virtuslab.gitmachete.graph.repositorygraph;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.model.BranchElement;
import com.virtuslab.gitmachete.graph.model.CommitElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
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
  @Nonnull @Setter private IBranchGetCommitsStrategy branchGetCommitsStrategy = DEFAULT_GET_COMMITS;

  public static IBranchGetCommitsStrategy DEFAULT_GET_COMMITS = IGitMacheteBranch::getCommits;
  public static IBranchGetCommitsStrategy EMPTY_GET_COMMITS = b -> Collections.emptyList();

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
      addCommitsWithBranch(graphElements, branch, /*upstreamBranchIndex*/ -1);
      addDownstreamCommitsAndBranches(graphElements, currentBranchIndex);
    }
    return graphElements;
  }

  /**
   * @param graphElements the collection to store downstream commits and branches
   * @param branchIndex the index of branch which downstream branches (with their commits) are to be
   *     added
   */
  private void addDownstreamCommitsAndBranches(List<IGraphElement> graphElements, int branchIndex)
      throws GitException {
    List<IGitMacheteBranch> branches =
        graphElements.get(branchIndex).getBranch().getDownstreamBranches();
    for (IGitMacheteBranch branch : branches) {
      int downElementIndex = graphElements.size();
      graphElements.get(branchIndex).getDownElementIndexes().add(downElementIndex);
      addCommitsWithBranch(graphElements, branch, branchIndex);

      int upstreamBranchIndex = graphElements.size() - 1;
      addDownstreamCommitsAndBranches(graphElements, upstreamBranchIndex);
    }
  }

  private void addCommitsWithBranch(
      List<IGraphElement> graphElements, IGitMacheteBranch branch, int upstreamBranchIndex)
      throws GitException {

    List<IGitMacheteCommit> commits = Lists.reverse(branchGetCommitsStrategy.getCommitsOf(branch));

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int lastElementIndex = graphElements.size() - 1;
      int upElementIndex = isFirstNodeInBranch ? upstreamBranchIndex : lastElementIndex;
      int downElementIndex = graphElements.size() + 1;
      CommitElement c = new CommitElement(commit, branch, upElementIndex, downElementIndex);
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

    BranchElement element = createBranchElementFor(branch, upElementIndex);
    graphElements.add(element);
  }

  /**
   * @return {@code BranchElement} for given {@code branch} and {@code upstreamBranchIndex} and
   *     provide additional attributes if the branch is the current one.
   */
  @Nonnull
  private BranchElement createBranchElementFor(IGitMacheteBranch branch, int upstreamBranchIndex) {
    BranchElement branchElement = new BranchElement(branch, upstreamBranchIndex);

    Optional<IGitMacheteBranch> currentBranch = Optional.empty();
    try {
      currentBranch = repository.getCurrentBranch();
    } catch (GitMacheteException e) {
      LOG.error("Unable to get current branch", e);
    }

    if (currentBranch.isPresent() && currentBranch.get().equals(branch)) {
      branchElement.setAttributes(BranchElement.UNDERLINE_BOLD_ATTRIBUTES);
    }

    return branchElement;
  }
}
