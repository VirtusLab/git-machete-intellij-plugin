package com.virtuslab.gitmachete.graph.repositorygraph;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.impl.print.PrintElementGeneratorImpl;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.facade.GraphElementManager;
import com.virtuslab.gitmachete.graph.model.IBranchElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

public abstract class BaseRepositoryGraph implements LinearGraph {
  @Nonnull private final PrintElementGeneratorImpl printElementGenerator;
  @Nonnull protected final List<IGraphElement> elements;
  @Nonnull private final IGitMacheteRepository repository;

  public BaseRepositoryGraph(@Nonnull IGitMacheteRepository repository) {
    this.repository = repository;
    this.elements = getGraphElementsOfRepository(repository);

    GraphElementManager myPrintElementManager = new GraphElementManager(/*repositoryGraph*/ this);
    printElementGenerator =
        new PrintElementGeneratorImpl(
            /*graph*/ this, myPrintElementManager, /*showLongEdges*/ false);
  }

  protected abstract List<IGraphElement> getGraphElementsOfRepository(
      @Nonnull IGitMacheteRepository repository);

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

  protected IBranchElement branchElementOf(
      IGitMacheteBranch gitMacheteBranch, int upstreamBranchIndex, int rowIndex) {
    IBranchElement branchElement =
        new IBranchElement(gitMacheteBranch, upstreamBranchIndex, rowIndex);

    IGitMacheteBranch currentBranch = null;
    try {
      if (repository.getCurrentBranch() != null) {
        currentBranch = repository.getCurrentBranch().orElse(null);
      }
    } catch (GitMacheteException e) {
      // Unable to get current branch
    }

    if (gitMacheteBranch.equals(currentBranch)) {
      branchElement.setAttributes(IBranchElement.UNDERLINE_BOLD_ATTRIBUTES);
    }

    return branchElement;
  }
}
