package com.virtuslab.gitmachete.graph.repositorygraph;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.impl.print.PrintElementGeneratorImpl;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.facade.GraphElementManager;
import com.virtuslab.gitmachete.graph.model.BranchElement;
import com.virtuslab.gitmachete.graph.model.CommitElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class RepositoryGraph implements LinearGraph {
  private static final Logger LOG = Logger.getInstance(RepositoryGraph.class);
  @Nonnull protected final List<IGraphElement> elements;
  @Nonnull private final PrintElementGeneratorImpl printElementGenerator;
  @Nonnull private final IGitMacheteRepository repository;
  @Nonnull private final IBranchGetCommitsAdapter branchGetCommitsAdapter;

  public static IBranchGetCommitsAdapter DEFAULT_GET_COMMITS = IGitMacheteBranch::getCommits;
  public static IBranchGetCommitsAdapter EMPTY_GET_COMMITS = b -> Collections.emptyList();

  public RepositoryGraph(
      @Nonnull IGitMacheteRepository repository,
      @Nonnull IBranchGetCommitsAdapter branchGetCommitsAdapter) {
    this.branchGetCommitsAdapter = branchGetCommitsAdapter;
    this.repository = repository;

    List<IGraphElement> tmpElements = Collections.emptyList();
    try {
      tmpElements = getGraphElementsOfRepository(repository);
    } catch (GitException e) {
      LOG.error("Unable to create repository graph", e);
    }
    this.elements = tmpElements;

    GraphElementManager printElementManager = new GraphElementManager(/*repositoryGraph*/ this);
    printElementGenerator =
        new PrintElementGeneratorImpl(/*graph*/ this, printElementManager, /*showLongEdges*/ false);
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
          .map(i -> GraphEdge.createNormalEdge(nodeIndex, i, GraphEdgeType.USUAL))
          .forEach(adjacentEdges::add);
    }

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = currentElement.getUpElementIndex();
      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  private List<IGraphElement> getGraphElementsOfRepository(
      @Nonnull IGitMacheteRepository repository) throws GitException {
    List<IGraphElement> graphElements = new ArrayList<>();
    List<IGitMacheteBranch> rootBranches = repository.getRootBranches();
    for (IGitMacheteBranch branch : rootBranches) {
      int currentBranchIndex = graphElements.size();
      addCommitsWithBranch(graphElements, branch, /*upstreamBranchIndex*/ -1);
      addDownstreamCommitsAndBranches(
          graphElements, branch, /*upstreamBranchIndex*/ currentBranchIndex);
    }
    return graphElements;
  }

  private void addDownstreamCommitsAndBranches(
      List<IGraphElement> graphElements, IGitMacheteBranch upstreamBranch, int upstreamBranchIndex)
      throws GitException {
    List<IGitMacheteBranch> branches = upstreamBranch.getDownstreamBranches();
    for (IGitMacheteBranch branch : branches) {
      graphElements.get(upstreamBranchIndex).getDownElementIndexes().add(graphElements.size());
      addCommitsWithBranch(graphElements, branch, upstreamBranchIndex);
      addDownstreamCommitsAndBranches(
          graphElements, branch, /*upstreamBranchIndex*/ graphElements.size() - 1);
    }
  }

  private void addCommitsWithBranch(
      List<IGraphElement> graphElements, IGitMacheteBranch branch, int upstreamBranchIndex)
      throws GitException {

    List<IGitMacheteCommit> commits = Lists.reverse(branchGetCommitsAdapter.getCommitsOf(branch));
    int branchIndex = upstreamBranchIndex + commits.size() + 1;

    boolean isFirstNodeInBranch = true;
    for (IGitMacheteCommit commit : commits) {
      int upElementIndex = isFirstNodeInBranch ? upstreamBranchIndex : graphElements.size() - 1;
      CommitElement c =
          new CommitElement(
              commit,
              branch,
              upElementIndex,
              branchIndex,
              /*downElementIndex*/ graphElements.size() + 1);
      graphElements.add(c);
      isFirstNodeInBranch = false;
    }

    int upElementIndex =
        upstreamBranchIndex == -1 || isFirstNodeInBranch
            ? upstreamBranchIndex
            : graphElements.size() - 1;
    BranchElement element = createBranchElementFor(branch, upElementIndex);
    graphElements.add(element);
  }

  private BranchElement createBranchElementFor(IGitMacheteBranch branch, int upstreamBranchIndex) {
    BranchElement branchElement = new BranchElement(branch, upstreamBranchIndex);

    IGitMacheteBranch currentBranch = null;
    try {
      currentBranch = repository.getCurrentBranch().orElse(null);
    } catch (GitMacheteException e) {
      // Unable to get current branch
      LOG.warn("Unable to get current branch", e);
    }

    if (branch.equals(currentBranch)) {
      branchElement.setAttributes(BranchElement.UNDERLINE_BOLD_ATTRIBUTES);
    }

    return branchElement;
  }
}
