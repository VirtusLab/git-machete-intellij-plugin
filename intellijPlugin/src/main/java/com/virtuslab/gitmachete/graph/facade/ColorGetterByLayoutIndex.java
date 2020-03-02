package com.virtuslab.gitmachete.graph.facade;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;

import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import com.intellij.vcs.log.graph.utils.NormalEdge;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;

@AllArgsConstructor
public class ColorGetterByLayoutIndex {
	@Nonnull
	private final RepositoryGraph repositoryGraph;

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

		IGraphElement graphElement = repositoryGraph.getGraphElement(nodeIndex);

		return graphElement.getGraphEdgeColor().getId();
	}
}
