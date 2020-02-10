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

  private List<IGraphElement> computeGraphElements() throws GitException {
    List<IGraphElement> graphElements = new ArrayList<>();
    List<IGitMacheteBranch> rootBranches = repository.getRootBranches();
    for (IGitMacheteBranch branch : rootBranches) {
      int currentBranchIndex = graphElements.size();
      addCommitsWithBranch(graphElements, branch, /*upstreamBranchIndex*/ -1);
      addDownstreamCommitsAndBranches(graphElements, /*upstreamBranchIndex*/ currentBranchIndex);
    }
    return graphElements;
  }

  private void addDownstreamCommitsAndBranches(
      List<IGraphElement> graphElements, int upstreamBranchIndex) throws GitException {
    List<IGitMacheteBranch> branches =
        graphElements.get(upstreamBranchIndex).getBranch().getDownstreamBranches();
    for (IGitMacheteBranch branch : branches) {
      graphElements.get(upstreamBranchIndex).getDownElementIndexes().add(graphElements.size());
      addCommitsWithBranch(graphElements, branch, upstreamBranchIndex);
      addDownstreamCommitsAndBranches(
          graphElements, /*upstreamBranchIndex*/ graphElements.size() - 1);
    }
  }

  private void addCommitsWithBranch(
      List<IGraphElement> graphElements, IGitMacheteBranch branch, int upstreamBranchIndex)
      throws GitException {

    List<IGitMacheteCommit> commits = Lists.reverse(branchGetCommitsStrategy.getCommitsOf(branch));

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int upElementIndex = isFirstNodeInBranch ? upstreamBranchIndex : graphElements.size() - 1;
      CommitElement c =
          new CommitElement(
              commit, branch, upElementIndex, /*downElementIndex*/ graphElements.size() + 1);
      graphElements.add(c);
      isFirstNodeInBranch = false;
    }

    int upElementIndex =
        upstreamBranchIndex == -1 || isFirstNodeInBranch
            ? upstreamBranchIndex
            : graphElements.size() - 1;
    BranchElement element = createBranchElementFor(branch, upElementIndex);
    graphElements.add(element);
  }

  /**
   * @return BranchElement for given branch and upstreamBranchIndex and provide additional
   *     attributes if the branch is the current one.
   */
  private BranchElement createBranchElementFor(IGitMacheteBranch branch, int upstreamBranchIndex) {
    BranchElement branchElement = new BranchElement(branch, upstreamBranchIndex);

    IGitMacheteBranch currentBranch = null;
    try {
      currentBranch = repository.getCurrentBranch().orElse(null);
    } catch (GitMacheteException e) {
      // Unable to get current branch
      LOG.error("Unable to get current branch", e);
    }

    if (branch.equals(currentBranch)) {
      branchElement.setAttributes(BranchElement.UNDERLINE_BOLD_ATTRIBUTES);
    }

    return branchElement;
  }
}
