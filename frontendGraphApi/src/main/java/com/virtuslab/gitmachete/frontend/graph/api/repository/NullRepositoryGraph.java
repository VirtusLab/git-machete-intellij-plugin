package com.virtuslab.gitmachete.frontend.graph.api.repository;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;

public class NullRepositoryGraph implements IRepositoryGraph {

  @Override
  public @NonNegative int getNodesCount() {
    return 0;
  }

  @Override
  public List<? extends IPrintElement> getPrintElements(@NonNegative int rowIndex) {
    throw new NotImplementedError();
  }

  @Override
  public IGraphItem getGraphItem(@NonNegative int rowIndex) {
    throw new NotImplementedError();
  }
}
