package com.virtuslab.gitmachete.frontend.graph.api.paint;

import javax.swing.JTable;

public interface IGraphCellPainterFactory {
  IGraphCellPainter create(JTable table);
}
