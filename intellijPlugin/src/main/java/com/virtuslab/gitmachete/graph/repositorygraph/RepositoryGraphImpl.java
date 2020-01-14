package com.virtuslab.gitmachete.graph.repositorygraph;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.impl.print.PrintElementGeneratorImpl;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.IGraphColorManager;
import com.virtuslab.gitmachete.graph.facade.GraphElementManagerImpl;
import com.virtuslab.gitmachete.graph.model.BranchElementI;
import com.virtuslab.gitmachete.graph.model.GraphElementI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RepositoryGraphImpl implements IRepositoryGraph {
  @Nonnull final List<GraphElementI> elements;
  private final PrintElementGeneratorImpl printElementGenerator;

  public RepositoryGraphImpl(@Nonnull IGitMacheteRepository repository) {
    elements = getGraphElementsOfRepository(repository);

    IGraphColorManager myColorManager =
        new IGraphColorManager() {
          final Random random = new Random();

          @Override
          public int getColor() {

            int r = (int) (155 + random.nextGaussian() * 100);
            int g = (int) ((1 - random.nextGaussian()) * (255 - r));
            int b = 255 - Math.min(g + r, 255);
            return r << 16 | g << 8 | b << 0;
          }
        };

    GraphElementManagerImpl myPrintElementManager =
        new GraphElementManagerImpl(this, myColorManager);
    printElementGenerator = new PrintElementGeneratorImpl(this, myPrintElementManager, false);
  }

  List<GraphElementI> getGraphElementsOfRepository(IGitMacheteRepository repository) {
    List<GraphElementI> graphElements = new ArrayList<>();
    for (IGitMacheteBranch branch : repository.getRootBranches()) {
      graphElements.add(new BranchElementI(branch));
      addDownstreamBranches(graphElements, branch);
    }
    return graphElements;
  }

  private void addDownstreamBranches(
      List<GraphElementI> graphElements, IGitMacheteBranch upstreamBranch) {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      graphElements.add(new BranchElementI(branch));
      addDownstreamBranches(graphElements, branch);
    }
  }

  @Override
  public Collection<? extends PrintElement> getPrintElements(int rowIndex) {
    return printElementGenerator.getPrintElements(rowIndex);
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
      IGitMacheteBranch branch = ((BranchElementI) currentElement).getBranch();
      adjacentEdges =
          branch.getBranches().stream()
              .map(
                  b ->
                      GraphEdge.createNormalEdge(
                          nodeIndex, elements.indexOf(new BranchElementI(b)), GraphEdgeType.USUAL))
              .collect(Collectors.toList());
    }

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = -1;

      if (currentElement instanceof BranchElementI) {
        // branch over branch
        upIndex = getUpstreamElementIndex((BranchElementI) currentElement);
      }

      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  int getUpstreamElementIndex(BranchElementI graphElement) {
    int upNode = -1;
    Optional<IGitMacheteBranch> upstreamBranch = graphElement.getBranch().getUpstreamBranch();
    if (upstreamBranch.isPresent()) {
      upNode = elements.indexOf(new BranchElementI(upstreamBranch.get()));
    }
    return upNode;
  }

  @Override
  public GraphElementI getGraphElement(int rowIndex) {
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
    return nodeIndex;
  }

  @Nullable
  @Override
  public Integer getNodeIndex(int nodeId) {
    return nodeId;
  }
}
