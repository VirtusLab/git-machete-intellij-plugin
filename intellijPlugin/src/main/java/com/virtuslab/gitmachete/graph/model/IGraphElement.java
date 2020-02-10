package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import java.util.List;

public interface IGraphElement {
  /**
   * @return The represented branch or (in case of representing a commit) a branch that contains the
   *     commit.
   */
  IGitMacheteBranch getBranch();

  /**
   * @return The index of element above (in table) that is connected directly in graph to this one.
   */
  int getUpElementIndex();

  /**
   * @return Indexes of elements below (in table) that are connected directly in graph to this one.
   */
  List<Integer> getDownElementIndexes();

  /** @return The text (commit msg/branch name) to be displayed in the table. */
  String getValue();

  /** @return Attributes (eg. boldness) to be used by the displayed text. */
  SimpleTextAttributes getAttributes();
}
