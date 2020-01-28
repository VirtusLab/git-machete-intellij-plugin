package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@EqualsAndHashCode
public final class IBranchElement implements IGraphElement {
  @Getter private final IGitMacheteBranch branch;

  public static final SimpleTextAttributes UNDERLINE_BOLD_ATTRIBUTES =
      new SimpleTextAttributes(
          SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_BOLD, /*fgColor*/ null);

  @Getter @Setter
  private SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

  @Override
  public String getValue() {
    return branch.getName();
  }
}
