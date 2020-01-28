package com.virtuslab.gitmachete.graph.facade;

import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import com.intellij.vcs.log.graph.utils.NormalEdge;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import com.virtuslab.gitmachete.graph.repositorygraph.BaseRepositoryGraph;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ColorGetterByLayoutIndex {
  @Nonnull private final BaseRepositoryGraph repositoryGraph;

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

    return colorOfSyncStatus(syncToParentStatus);
  }

  private int colorOfSyncStatus(@Nullable SyncToParentStatus syncToParentStatus) {
    if (syncToParentStatus == null) {
      return 0;
    }

    switch (syncToParentStatus) {
      case Merged:
        return 0;
      case NotADirectDescendant:
        return 1;
      case OutOfSync:
        return 2;
      case InSync:
        return 3;
      default:
        return 0;
    }
  }
}
