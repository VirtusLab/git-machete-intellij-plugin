package com.virtuslab.gitmachete.graph.repositorygraph;

import com.google.common.collect.Lists;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.model.BranchElementI;
import com.virtuslab.gitmachete.graph.model.CommitElementI;
import com.virtuslab.gitmachete.graph.model.GraphElementI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class RepositoryGraphWithCommitsImpl extends RepositoryGraphImpl {
  public RepositoryGraphWithCommitsImpl(@Nonnull IGitMacheteRepository repository) {
    super(repository);
  }

  @Override
  protected List<GraphElementI> getGraphElementsOfRepository(IGitMacheteRepository repository) {
    List<GraphElementI> graphElements = new ArrayList<>();
    for (IGitMacheteBranch branch : repository.getRootBranches()) {
      try {
        branch.getCommits().stream().map(CommitElementI::new).forEach(graphElements::add);
        graphElements.add(new BranchElementI(branch));
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
      List<GraphElementI> graphElements, IGitMacheteBranch upstreamBranch) throws GitException {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      Lists.reverse(branch.getCommits()).stream()
          .map(CommitElementI::new)
          .forEach(graphElements::add);
      graphElements.add(new BranchElementI(branch));
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

    GraphElementI currentElement = elements.get(nodeIndex);

    if (filter.downNormal && nodeIndex < elements.size() - 1) {
      if (currentElement instanceof CommitElementI) {
        adjacentEdges.add(
            GraphEdge.createNormalEdge(nodeIndex, nodeIndex + 1, GraphEdgeType.USUAL));
      } else {
        IGitMacheteBranch branch = ((BranchElementI) currentElement).getBranch();
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
      GraphElementI upElement = elements.get(nodeIndex - 1);
      int upIndex = -1;

      if (upElement instanceof CommitElementI) {
        // commit over branch/commit
        upIndex = nodeIndex - 1;
      } else if (currentElement instanceof BranchElementI) {
        // branch over branch
        upIndex = getUpstreamElementIndex((BranchElementI) currentElement);
      } else if (currentElement instanceof CommitElementI) {
        // branch over commit
        Optional<GraphElementI> branch = getNextBranchElement(nodeIndex);
        if (branch.isPresent()) {
          upIndex = getUpstreamElementIndex((BranchElementI) branch.get());
        }
      }

      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  private int getUpstreamBranchElementIndex(IGitMacheteBranch b) {
    int idx = elements.indexOf(new BranchElementI(b));
    try {
      idx -= b.getCommits().size();
    } catch (GitException e) {
      e.printStackTrace();
    }
    return idx;
  }

  @Nonnull
  private Optional<GraphElementI> getNextBranchElement(int elementIndex) {
    return elements.stream()
        .skip(elementIndex + 1)
        .filter(BranchElementI.class::isInstance)
        .findFirst();
  }
}
