package com.virtuslab.gitmachete.graph.facade;

import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.virtuslab.gitmachete.graph.IGraphColorManager;
import javax.annotation.Nonnull;

public class ColorGetterByLayoutIndex {
  @Nonnull private final IGraphColorManager myColorManager;

  public ColorGetterByLayoutIndex(@Nonnull IGraphColorManager colorManager) {
    myColorManager = colorManager;
  }

  public int getColorId(@Nonnull GraphElement element) {
    /* todo: implement proper coloring */

    return myColorManager.getColor();
  }
}
