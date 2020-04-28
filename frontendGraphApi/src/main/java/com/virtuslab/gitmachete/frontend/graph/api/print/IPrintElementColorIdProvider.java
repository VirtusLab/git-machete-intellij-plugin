package com.virtuslab.gitmachete.frontend.graph.api.print;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;

public interface IPrintElementColorIdProvider {
  int getColorId(IGraphElement element);
}
