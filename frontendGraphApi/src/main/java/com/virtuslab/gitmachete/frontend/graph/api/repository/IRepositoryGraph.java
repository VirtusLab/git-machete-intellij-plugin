package com.virtuslab.gitmachete.frontend.graph.api.repository;

import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;

public interface IRepositoryGraph {

  @NonNegative
  int getNodesCount();

  List<? extends IPrintElement> getPrintElements(@NonNegative int rowIndex);

  IGraphItem getGraphItem(@NonNegative int rowIndex);
}
