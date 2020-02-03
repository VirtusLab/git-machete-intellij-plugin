package com.virtuslab.gitmachete.graph.repositorygraph;

import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.model.IBranchElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class RepositoryGraph extends BaseRepositoryGraph {

  public RepositoryGraph(@Nonnull IGitMacheteRepository repository) {
    super(repository);
  }

  @Override
  List<IGraphElement> getGraphElementsOfRepository(@Nonnull IGitMacheteRepository repository) {
    List<IGraphElement> graphElements = new ArrayList<>();
    for (IGitMacheteBranch branch : repository.getRootBranches()) {
      graphElements.add(new IBranchElement(branch));
      addDownstreamBranches(graphElements, branch);
    }
    return graphElements;
  }

  private void addDownstreamBranches(
      List<IGraphElement> graphElements, IGitMacheteBranch upstreamBranch) {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      graphElements.add(new IBranchElement(branch));
      addDownstreamBranches(graphElements, branch);
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
      IGitMacheteBranch branch = ((IBranchElement) currentElement).getBranch();
      adjacentEdges =
          branch.getBranches().stream()
              .map(
                  b ->
                      GraphEdge.createNormalEdge(
                          nodeIndex, elements.indexOf(new IBranchElement(b)), GraphEdgeType.USUAL))
              .collect(Collectors.toList());
    }

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = -1;

      if (currentElement instanceof IBranchElement) {
        // branch over branch
        upIndex = getUpstreamElementIndex((IBranchElement) currentElement);
      }

      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }
}
