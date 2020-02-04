package com.virtuslab.gitmachete.graph.repositorygraph;

import com.google.common.collect.Lists;
import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.model.IBranchElement;
import com.virtuslab.gitmachete.graph.model.ICommitElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class RepositoryGraphWithCommits extends BaseRepositoryGraph {
  public RepositoryGraphWithCommits(@Nonnull IGitMacheteRepository repository) {
    super(repository);
  }

  @Override
  protected List<IGraphElement> getGraphElementsOfRepository(
      @Nonnull IGitMacheteRepository repository) {
    List<IGraphElement> graphElements = new ArrayList<>();
    List<IGitMacheteBranch> rootBranches = repository.getRootBranches();
    for (IGitMacheteBranch branch : rootBranches) {
      int currentBranchIndex = graphElements.size();
      boolean commitsSuccessfullyAdded =
          addCommitsWithBranch(graphElements, branch, /*upstreamBranchIndex*/ -1);
      boolean downstreamElementsSuccessfullyAdded =
          addDownstreamCommitsAndBranches(
              graphElements, branch, /*upstreamBranchIndex*/ currentBranchIndex);

      if (!(commitsSuccessfullyAdded && downstreamElementsSuccessfullyAdded)) {
        // Unable to get commits of a branch
        graphElements.clear();
        break;
      }
    }
    return graphElements;
  }

  // returns false when getCommits throws an exception, otherwise true
  private boolean addDownstreamCommitsAndBranches(
      List<IGraphElement> graphElements,
      IGitMacheteBranch upstreamBranch,
      int upstreamBranchIndex) {
    List<IGitMacheteBranch> branches = upstreamBranch.getBranches();
    for (IGitMacheteBranch branch : branches) {
      ((IBranchElement) graphElements.get(upstreamBranchIndex))
          .getDownElementsIndexes()
          .add(graphElements.size());

      boolean commitsSuccessfullyAdded =
          addCommitsWithBranch(graphElements, branch, /*upstreamBranchIndex*/ upstreamBranchIndex);
      boolean downstreamElementsSuccessfullyAdded =
          addDownstreamCommitsAndBranches(
              graphElements, branch, /*upstreamBranchIndex*/ graphElements.size() - 1);

      if (!(commitsSuccessfullyAdded && downstreamElementsSuccessfullyAdded)) {
        return false;
      }
    }
    return true;
  }

  // returns false when getCommits throws an exception, otherwise true
  private boolean addCommitsWithBranch(
      List<IGraphElement> graphElements, IGitMacheteBranch branch, int upstreamBranchIndex) {
    List<IGitMacheteCommit> commits;

    try {
      commits = Lists.reverse(branch.getCommits());
    } catch (GitException e) {
      return false;
    }

    int commitsBranchIndex = upstreamBranchIndex + commits.size() + 1;

    if (commits.size() > 0) {
      ICommitElement c =
          new ICommitElement(commits.get(0), branch, upstreamBranchIndex, commitsBranchIndex);
      graphElements.add(c);
    }

    for (int i = 1, commitsSize = commits.size(); i < commitsSize; i++) {
      IGitMacheteCommit iGitMacheteCommit = commits.get(i);
      ICommitElement c =
          new ICommitElement(
              iGitMacheteCommit, branch, graphElements.size() - 1, commitsBranchIndex);
      graphElements.add(c);
    }

    int upElementIndex = upstreamBranchIndex == -1 ? upstreamBranchIndex : graphElements.size() - 1;
    IBranchElement element =
        branchElementOf(
            branch, /*upElementIndex*/ upElementIndex, /*rowIndex*/ graphElements.size());
    graphElements.add(element);

    return true;
  }

  @Nonnull
  @Override
  public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
    if (filter == EdgeFilter.SPECIAL) {
      return Collections.emptyList();
    }

    List<GraphEdge> adjacentEdges = new SmartList<>();
    IGraphElement currentElement = elements.get(nodeIndex);

    if (filter.downNormal && nodeIndex < elements.size() - 1) {
      if (currentElement instanceof ICommitElement) {
        adjacentEdges.add(
            GraphEdge.createNormalEdge(nodeIndex, nodeIndex + 1, GraphEdgeType.USUAL));
      } else {
        List<Integer> downElementsIndexes =
            ((IBranchElement) currentElement).getDownElementsIndexes();
        downElementsIndexes.stream()
            .map(i -> GraphEdge.createNormalEdge(nodeIndex, i, GraphEdgeType.USUAL))
            .forEach(adjacentEdges::add);
      }
    }

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = currentElement.getUpElementIndex();
      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }
}
