package com.virtuslab.gitmachete.graph.facade;

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
    try {
      IGitMacheteBranch branch = repositoryGraph.getGraphElement(nodeIndex).getBranch();
      syncToParentStatus = branch.getSyncToParentStatus();
    } catch (GitException e) {
      e.printStackTrace();
    }

    return syncToParentStatus == null ? 0 : syncToParentStatus.getColorId();
  }
}
