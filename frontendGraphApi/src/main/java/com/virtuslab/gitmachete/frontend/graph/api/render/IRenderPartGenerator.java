package com.virtuslab.gitmachete.frontend.graph.api.render;

import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;

public interface IRenderPartGenerator {
  List<? extends IRenderPart> getRenderParts(@NonNegative int rowIndex);
}
