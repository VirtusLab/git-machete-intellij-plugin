package com.virtuslab.gitmachete.graph.repositorygraph;

import com.google.common.collect.Lists;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.model.IBranchElement;
import com.virtuslab.gitmachete.graph.model.ICommitElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class RepositoryGraphWithCommitsImpl extends RepositoryGraph {
  public RepositoryGraphWithCommitsImpl(@Nonnull IGitMacheteRepository repository) {
    super(repository);
  }

  @Override
  protected List<IGraphElement> getGraphElementsOfRepository(
      @Nonnull IGitMacheteRepository repository) {
    List<IGraphElement> graphElements = new ArrayList<>();
    for (IGitMacheteBranch branch : repository.getRootBranches()) {
      try {
        branch.getCommits().stream().map(ICommitElement::new).forEach(graphElements::add);
        graphElements.add(new IBranchElement(branch));
        addDownstreamBranchesAndCommits(graphElements, branch);
      } catch (GitException e) {
        // Unable to get commits of a branch
        graphElements.clear();
        break;
      }
    }
    return graphElements;
  }

  private void addDownstreamBranchesAndCommits(
      List<IGraphElement> graphElements, IGitMacheteBranch upstreamBranch) throws GitException {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      Lists.reverse(branch.getCommits()).stream()
          .map(ICommitElement::new)
          .forEach(graphElements::add);
      graphElements.add(new IBranchElement(branch));
      addDownstreamBranchesAndCommits(graphElements, branch);
    }
  }

  @Nonnull
  @Override
  public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
    if (filter == EdgeFilter.SPECIAL) {
      return Collections.emptyList();
    }

    List<GraphEdge> adjacentEdges = new ArrayList<>();

    IGraphElement currentElement = elements.get(nodeIndex);

    if (filter.downNormal && nodeIndex < elements.size() - 1) {
      if (currentElement instanceof ICommitElement) {
        adjacentEdges.add(
            GraphEdge.createNormalEdge(nodeIndex, nodeIndex + 1, GraphEdgeType.USUAL));
      } else {
        IGitMacheteBranch branch = ((IBranchElement) currentElement).getBranch();
        adjacentEdges =
            branch.getBranches().stream()
                .map(
                    b ->
                        GraphEdge.createNormalEdge(
                            nodeIndex, getUpstreamBranchElementIndex(b), GraphEdgeType.USUAL))
                .collect(Collectors.toList());
      }
    }

    if (filter.upNormal && nodeIndex > 0) {
      IGraphElement upElement = elements.get(nodeIndex - 1);
      int upIndex = -1;

      if (upElement instanceof ICommitElement) {
        // commit over branch/commit
        upIndex = nodeIndex - 1;
      } else if (currentElement instanceof IBranchElement) {
        // branch over branch
        upIndex = getUpstreamElementIndex((IBranchElement) currentElement);
      } else if (currentElement instanceof ICommitElement) {
        // branch over commit
        Optional<IGraphElement> branch = getNextBranchElement(nodeIndex);
        if (branch.isPresent()) {
          upIndex = getUpstreamElementIndex((IBranchElement) branch.get());
        }
      }

      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  private int getUpstreamBranchElementIndex(IGitMacheteBranch b) {
    int idx = elements.indexOf(new IBranchElement(b));
    try {
      idx -= b.getCommits().size();
    } catch (GitException e) {
      e.printStackTrace();
    }
    return idx;
  }

  @Nonnull
  private Optional<IGraphElement> getNextBranchElement(int elementIndex) {
    return elements.stream()
        .skip(elementIndex + 1)
        .filter(IBranchElement.class::isInstance)
        .findFirst();
  }
}
