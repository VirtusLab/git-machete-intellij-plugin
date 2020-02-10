package com.virtuslab.gitmachete.graph.facade;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import com.intellij.vcs.log.graph.utils.NormalEdge;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ColorGetterByLayoutIndex {
  private static final Logger LOG = Logger.getInstance(ColorGetterByLayoutIndex.class);
  @Nonnull private final RepositoryGraph repositoryGraph;

  public int getColorId(@Nonnull GraphElement element) {
    int nodeIndex;
    if (element instanceof GraphNode) {
      nodeIndex = ((GraphNode) element).getNodeIndex();
    } else {
      GraphEdge edge = (GraphEdge) element;
      NormalEdge normalEdge = LinearGraphUtils.asNormalEdge(edge);
      if (normalEdge != null) {
        nodeIndex = normalEdge.down;
      } else {
        nodeIndex = LinearGraphUtils.getNotNullNodeIndex(edge);
      }
    }

    SyncToParentStatus syncToParentStatus = null;
    IGitMacheteBranch branch = repositoryGraph.getGraphElement(nodeIndex).getBranch();
    // todo use vavr Try
    try {
      syncToParentStatus = branch.getSyncToParentStatus();
    } catch (GitException e) {
      LOG.error("Unable to get a sync to parent status", e);
    }

    return syncToParentStatus == null ? 0 : syncToParentStatus.getColorId();
  }
}
