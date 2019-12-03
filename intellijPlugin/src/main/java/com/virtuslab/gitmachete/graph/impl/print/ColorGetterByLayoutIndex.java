package com.virtuslab.gitmachete.graph.impl.print;

import com.virtuslab.gitmachete.api.IGraphColorManager;
import javax.annotation.Nonnull;

/* todo: implement proper coloring */
public class ColorGetterByLayoutIndex {
  @Nonnull private final IGraphColorManager myColorManager;

  public ColorGetterByLayoutIndex(@Nonnull IGraphColorManager colorManager) {
    myColorManager = colorManager;
  }

  public int getColorId() {
    return myColorManager.getColor();
  }
}
