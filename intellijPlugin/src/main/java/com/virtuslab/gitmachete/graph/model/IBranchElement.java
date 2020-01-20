package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
public final class IBranchElement implements IGraphElement {
  @Getter private final IGitMacheteBranch branch;

  @Override
  public String getValue() {
    return branch.getName();
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
  }
}
