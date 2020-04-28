package com.virtuslab.gitmachete.frontend.graph.api.render;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;

public interface IRenderPartColorIdProvider {
  int getColorId(IGraphElement element);
}
