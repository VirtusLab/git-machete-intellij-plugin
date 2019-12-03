package com.virtuslab.gitmachete.data;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.impl.print.PrintElementGeneratorImpl;
import com.virtuslab.gitmachete.api.IGraphColorManager;
import com.virtuslab.gitmachete.api.IRepositoryGraph;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.impl.facade.PrintElementManagerImpl;
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
  @Nonnull private final List<IGitMacheteBranch> myBranchesList; // todo: consider using SmartList
  private final PrintElementGeneratorImpl myPrintElementGenerator;
  private final IGraphColorManager myColorManager;

  public RepositoryGraphImpl(@Nonnull IGitMacheteRepository repository) {
    myBranchesList = toList(repository);
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
    PrintElementManagerImpl myPrintElementManager =
        new PrintElementManagerImpl(this, myColorManager);
    myPrintElementGenerator = new PrintElementGeneratorImpl(this, myPrintElementManager, false);
  }

  private List<IGitMacheteBranch> toList(IGitMacheteRepository repository) {
    List<IGitMacheteBranch> list = new ArrayList<>();
    for (IGitMacheteBranch branch : repository.getRootBranches()) {
      list.add(branch);
      addDownstreamBranches(list, branch);
    }
    return list;
  }

  private void addDownstreamBranches(
      List<IGitMacheteBranch> list, IGitMacheteBranch upstreamBranch) {
    for (IGitMacheteBranch branch : upstreamBranch.getBranches()) {
      list.add(branch);
      addDownstreamBranches(list, branch);
    }
  }

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

    if (filter.downNormal) {
      adjacentEdges =
          myBranchesList.get(nodeIndex).getBranches().stream()
              .map(
                  b ->
                      GraphEdge.createNormalEdge(
                          nodeIndex, myBranchesList.indexOf(b), GraphEdgeType.USUAL))
              .collect(Collectors.toList());
    }

    if (filter.upNormal) {
      Optional<IGitMacheteBranch> upstreamBranch =
          myBranchesList.get(nodeIndex).getUpstreamBranch();
      if (upstreamBranch.isPresent()) {
        GraphEdge normalEdge =
            GraphEdge.createNormalEdge(
                nodeIndex, myBranchesList.indexOf(upstreamBranch.get()), GraphEdgeType.USUAL);
        adjacentEdges.add(normalEdge);
      }
    }

    return adjacentEdges;
  }

  @Override
  public IGitMacheteBranch getBranch(int rowIndex) {
    return myBranchesList.get(rowIndex);
  }

  @Override
  public int nodesCount() {
    return myBranchesList.size();
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
