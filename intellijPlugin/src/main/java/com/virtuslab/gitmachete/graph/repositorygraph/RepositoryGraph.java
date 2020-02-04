package com.virtuslab.gitmachete.graph.repositorygraph;

import com.intellij.util.SmartList;
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
import javax.annotation.Nonnull;

public class RepositoryGraph extends BaseRepositoryGraph {

  public RepositoryGraph(@Nonnull IGitMacheteRepository repository) {
    super(repository);
  }

  @Override
  protected List<IGraphElement> getGraphElementsOfRepository(
      @Nonnull IGitMacheteRepository repository) {
    List<IGraphElement> graphElements = new ArrayList<>();
    List<IGitMacheteBranch> rootBranches = repository.getRootBranches();
    for (IGitMacheteBranch branch : rootBranches) {
      IBranchElement element =
          branchElementOf(branch, /*upstreamBranchIndex*/ -1, /*rowIndex*/ graphElements.size());
      graphElements.add(element);
      addDownstreamBranches(
          graphElements, branch, /*upstreamBranchIndex*/ graphElements.size() - 1);
    }
    return graphElements;
  }

  private void addDownstreamBranches(
      List<IGraphElement> graphElements,
      IGitMacheteBranch upstreamBranch,
      int upstreamBranchIndex) {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      IBranchElement element =
          branchElementOf(branch, upstreamBranchIndex, /*rowIndex*/ graphElements.size());
      ((IBranchElement) graphElements.get(upstreamBranchIndex))
          .getDownElementsIndexes()
          .add(graphElements.size());
      graphElements.add(element);
      addDownstreamBranches(
          graphElements, branch, /*upstreamBranchIndex*/ graphElements.size() - 1);
    }
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
      List<Integer> downElementsIndexes =
          ((IBranchElement) currentElement).getDownElementsIndexes();
      downElementsIndexes.stream()
          .map(i -> GraphEdge.createNormalEdge(nodeIndex, i, GraphEdgeType.USUAL))
          .forEach(adjacentEdges::add);
    }

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = -1;

      if (currentElement instanceof IBranchElement) {
        // branch over branch
        upIndex = currentElement.getUpElementIndex();
      }

      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }
}
