package com.virtuslab.gitmachete.graph.repositorygraph;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.impl.print.PrintElementGeneratorImpl;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.IGraphColorManager;
import com.virtuslab.gitmachete.graph.facade.GraphElementManager;
import com.virtuslab.gitmachete.graph.model.IBranchElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nonnull;

public abstract class BaseRepositoryGraph implements LinearGraph {
  @Nonnull private final PrintElementGeneratorImpl printElementGenerator;
  @Nonnull protected final List<IGraphElement> elements;

  public BaseRepositoryGraph(@Nonnull IGitMacheteRepository repository) {
    this.elements = getGraphElementsOfRepository(repository);

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

    GraphElementManager myPrintElementManager =
        new GraphElementManager(/*linearGraph*/ this, myColorManager);
    printElementGenerator =
        new PrintElementGeneratorImpl(
            /*graph*/ this, myPrintElementManager, /*showLongEdges*/ false);
  }

  abstract List<IGraphElement> getGraphElementsOfRepository(
      @Nonnull IGitMacheteRepository repository);

  public Collection<? extends PrintElement> getPrintElements(int rowIndex) {
    return printElementGenerator.getPrintElements(rowIndex);
  }

  public IGraphElement getGraphElement(int rowIndex) {
    return elements.get(rowIndex);
  }

  protected int getUpstreamElementIndex(@Nonnull IBranchElement graphElement) {
    int upNode = -1;
    Optional<IGitMacheteBranch> upstreamBranch = graphElement.getBranch().getUpstreamBranch();
    if (upstreamBranch.isPresent()) {
      upNode = elements.indexOf(new IBranchElement(upstreamBranch.get()));
    }
    return upNode;
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
}
