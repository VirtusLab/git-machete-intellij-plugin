package com.virtuslab.gitmachete.frontend.graph.api.paint;

import java.awt.Graphics2D;

import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;

public interface IGraphCellPainter {

  @UIEffect
  void draw(Graphics2D g2, List<? extends IRenderPart> renderParts);
}
