package com.virtuslab.gitmachete.graph.repositoryGraph;

import com.google.common.collect.Lists;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.model.BranchElement;
import com.virtuslab.gitmachete.graph.model.CommitElement;
import com.virtuslab.gitmachete.graph.model.GraphElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class RepositoryGraphWithCommits extends RepositoryGraphImpl {
  public RepositoryGraphWithCommits(@Nonnull IGitMacheteRepository repository) {
    super(repository);
  }

  @Override
  protected List<GraphElement> getGraphElementsOfRepository(IGitMacheteRepository repository) {
    List<GraphElement> graphElements = new ArrayList<>();
    for (IGitMacheteBranch branch : repository.getRootBranches()) {
      try {
        branch.getCommits().stream().map(CommitElement::new).forEach(graphElements::add);
        graphElements.add(new BranchElement(branch));
        addDownstreamBranchesAndCommits(graphElements, branch);
      } catch (GitException e) {
        e.printStackTrace();
        graphElements.clear();
        break;
      }
    }
    return graphElements;
  }

  private void addDownstreamBranchesAndCommits(
      List<GraphElement> elements, IGitMacheteBranch upstreamBranch) throws GitException {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      Lists.reverse(branch.getCommits()).stream().map(CommitElement::new).forEach(elements::add);
      elements.add(new BranchElement(branch));
      addDownstreamBranchesAndCommits(elements, branch);
    }
  }

  @Nonnull
  @Override
  public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
    if (filter == EdgeFilter.SPECIAL) {
      return Collections.emptyList();
    }

    List<GraphEdge> adjacentEdges = new ArrayList<>();

    GraphElement currentElement = elements.get(nodeIndex);

    if (filter.downNormal && nodeIndex < elements.size() - 1) {
      if (currentElement instanceof CommitElement) {
        adjacentEdges.add(
            GraphEdge.createNormalEdge(nodeIndex, nodeIndex + 1, GraphEdgeType.USUAL));
      } else {
        IGitMacheteBranch branch = ((BranchElement) currentElement).getBranch();
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
      GraphElement upElement = elements.get(nodeIndex - 1);
      int upIndex = -1;

      if (upElement instanceof CommitElement) {
        // commit over branch/commit
        upIndex = nodeIndex - 1;
      } else if (currentElement instanceof BranchElement) {
        // branch over branch
        upIndex = getUpstreamElementIndex((BranchElement) currentElement);
      } else if (currentElement instanceof CommitElement) {
        // branch over commit
        Optional<GraphElement> branch = getNextBranchElement(nodeIndex);
        if (branch.isPresent()) {
          upIndex = getUpstreamElementIndex((BranchElement) branch.get());
        }
      }

      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  private int getUpstreamBranchElementIndex(IGitMacheteBranch b) {
    int idx = elements.indexOf(new BranchElement(b));
    try {
      idx -= b.getCommits().size();
    } catch (GitException e) {
      e.printStackTrace();
    }
    return idx;
  }

  @Nonnull
  private Optional<GraphElement> getNextBranchElement(int elementIndex) {
    return elements.stream()
        .skip(elementIndex + 1)
        .filter(BranchElement.class::isInstance)
        .findFirst();
  }
}
