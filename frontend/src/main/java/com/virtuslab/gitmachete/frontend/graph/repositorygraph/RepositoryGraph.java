package com.virtuslab.gitmachete.frontend.graph.repositorygraph;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.Getter;

import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.impl.print.PrintElementGeneratorImpl;

import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.frontend.graph.facade.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.model.IGraphElement;

public class RepositoryGraph implements LinearGraph {
  @Nonnull
  @Getter
  private static final RepositoryGraph nullRepositoryGraph = new RepositoryGraphBuilder()
      .repository(NullRepository.getInstance()).build();

  @Nonnull
  private final List<IGraphElement> elements;
  @Nonnull
  private final PrintElementGeneratorImpl printElementGenerator;

  public RepositoryGraph(@Nonnull List<IGraphElement> elements) {
    this.elements = elements;

    GraphElementManager printElementManager = new GraphElementManager(/* repositoryGraph */ this);
    printElementGenerator = new PrintElementGeneratorImpl(/* graph */ this, printElementManager,
        /* showLongEdges */ false);
  }

  public Collection<? extends PrintElement> getPrintElements(int rowIndex) {
    return printElementGenerator.getPrintElements(rowIndex);
  }

  public IGraphElement getGraphElement(int rowIndex) {
    return elements.get(rowIndex);
  }

  @Override
  public int nodesCount() {
    return elements.size();
  }

  @Nonnull
  @Override
  public GraphNode getGraphNode(int nodeIndex) {
    return new GraphNode(nodeIndex);
  }

  @Override
  public int getNodeId(int nodeIndex) {
    assert nodeIndex >= 0 && nodeIndex < nodesCount() : "Bad nodeIndex: " + nodeIndex;
    return nodeIndex;
  }

  @Override
  public Integer getNodeIndex(int nodeId) {
    if (nodeId >= 0 && nodeId < nodesCount()) {
      return nodeId;
    }
    return null;
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
      currentElement.getDownElementIndexes().stream()
          .map(i -> GraphEdge.createNormalEdge(nodeIndex, i, GraphEdgeType.USUAL)).forEach(adjacentEdges::add);
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
