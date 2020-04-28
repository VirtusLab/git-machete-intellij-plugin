package com.virtuslab.gitmachete.frontend.graph.api.repository;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;

public interface IRepositoryGraph {

  List<GraphEdge> getAdjacentEdges(@NonNegative int itemIndex);

  IGraphItem getGraphItem(@NonNegative int itemIndex);

  @NonNegative
  int getNodesCount();

  List<? extends IPrintElement> getPrintElements(@NonNegative int itemIndex);

  List<Tuple2<GraphEdge, @NonNegative Integer>> getVisibleEdgesWithPositions(@NonNegative int itemIndex);
}
