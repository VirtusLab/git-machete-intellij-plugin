package com.virtuslab.gitmachete.frontend.graph.impl.paint;

import javax.swing.JTable;

import com.virtuslab.gitmachete.frontend.graph.api.paint.IColorProvider;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainterFactory;

public class GraphCellPainterFactory implements IGraphCellPainterFactory {
  @Override
  public GraphCellPainter create(IColorProvider colorProvider, JTable table) {
    return new GraphCellPainter(colorProvider, table);
  }
}
