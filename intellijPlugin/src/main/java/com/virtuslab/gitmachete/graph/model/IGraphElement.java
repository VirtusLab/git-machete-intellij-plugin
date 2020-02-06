package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import java.util.List;

public interface IGraphElement {
  IGitMacheteBranch getBranch();

  /*
   * an up element index is an index of element higher (in table)
   * that is connected in graph to this one
   */
  int getUpElementIndex();

  /*
   * down element indexes are indexes of elements lower (in table)
   * that are connected in graph to this one
   */
  List<Integer> getDownElementIndexes();

  String getValue();

  SimpleTextAttributes getAttributes();
}
