package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;

public interface IGraphElement {
  IGitMacheteBranch getBranch();

  int getUpElementIndex();

  String getValue();

  SimpleTextAttributes getAttributes();
}
