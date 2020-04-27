package com.virtuslab.gitmachete.frontend.graph.api.repository;

import java.util.Collection;

import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;

public interface IRepositoryGraph {

  @NonNegative
  int nodesCount();

  Collection<? extends IPrintElement> getPrintElements(@NonNegative int rowIndex);

  IGraphItem getGraphItem(@NonNegative int rowIndex);
}
