package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
public final class BranchElement extends BaseGraphElement {

  public BranchElement(IGitMacheteBranch branch, int upElementIndex) {
    super(branch, upElementIndex);
  }

  public static final SimpleTextAttributes UNDERLINE_BOLD_ATTRIBUTES =
      new SimpleTextAttributes(
          SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_BOLD, /*fgColor*/ null);

  @Getter @Setter
  private SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

  @Override
  public String getValue() {
    return super.getBranch().getName();
  }
}
