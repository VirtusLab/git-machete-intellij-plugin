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
import com.virtuslab.gitmachete.graph.model.BranchElement;
import com.virtuslab.gitmachete.graph.model.GraphElement;
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
  @Nonnull private final List<GraphElement> myElements;
  private final PrintElementGeneratorImpl myPrintElementGenerator;
  private final IGraphColorManager myColorManager;

  public RepositoryGraphImpl(@Nonnull IGitMacheteRepository repository) {
    myElements = toList(repository);
    myColorManager =
        new IGraphColorManager() {
          Random random = new Random();

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
    myPrintElementGenerator = new PrintElementGeneratorImpl(this, myPrintElementManager, false);
  }

  private List<GraphElement> toList(IGitMacheteRepository repository) {
    List<GraphElement> list = new ArrayList<>();
    for (IGitMacheteBranch branch : repository.getRootBranches()) {
      list.add(new BranchElement(branch));
      addDownstreamBranches(list, branch);
    }
    return list;
  }

  private void addDownstreamBranches(List<GraphElement> list, IGitMacheteBranch upstreamBranch) {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      list.add(new BranchElement(branch));
      addDownstreamBranches(list, branch);
    }
  }

  @Override
  public Collection<? extends PrintElement> getPrintElements(int rowIndex) {
    return myPrintElementGenerator.getPrintElements(rowIndex);
  }

  @Nonnull
  @Override
  public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
    if (filter == EdgeFilter.SPECIAL) {
      return Collections.emptyList();
    }

    List<GraphEdge> adjacentEdges = new ArrayList<>();

    GraphElement currentElement = myElements.get(nodeIndex);

    if (filter.downNormal && nodeIndex < myElements.size() - 1) {
      IGitMacheteBranch branch = ((BranchElement) currentElement).getBranch();
      adjacentEdges =
          branch.getBranches().stream()
              .map(
                  b ->
                      GraphEdge.createNormalEdge(
                          nodeIndex, myElements.indexOf(new BranchElement(b)), GraphEdgeType.USUAL))
              .collect(Collectors.toList());
    }

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = -1;

      if (currentElement instanceof BranchElement) {
        // branch over branch
        upIndex = getUpstreamElementIndex((BranchElement) currentElement);
      }

      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  private int getUpstreamElementIndex(BranchElement graphElement) {
    int upNode = -1;
    Optional<IGitMacheteBranch> upstreamBranch = graphElement.getBranch().getUpstreamBranch();
    if (upstreamBranch.isPresent()) {
      upNode = myElements.indexOf(new BranchElement(upstreamBranch.get()));
    }
    return upNode;
  }

  @Override
  public GraphElement getGraphElement(int rowIndex) {
    return myElements.get(rowIndex);
  }

  @Override
  public int nodesCount() {
    return myElements.size();
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
