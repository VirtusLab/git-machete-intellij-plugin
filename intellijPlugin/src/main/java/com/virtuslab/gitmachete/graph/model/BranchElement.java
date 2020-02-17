package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
public final class BranchElement extends BaseGraphElement {
  @Getter private final IGitMacheteBranch branch;
  @Getter private final SyncToOriginStatus syncToOriginStatus;

  public BranchElement(
      IGitMacheteBranch branch,
      int upElementIndex,
      SyncToParentStatus syncToParentStatus,
      SyncToOriginStatus syncToOriginStatus) {
    super(upElementIndex, syncToParentStatus);
    this.branch = branch;
    this.syncToOriginStatus = syncToOriginStatus;
  }

  public static final SimpleTextAttributes UNDERLINE_BOLD_ATTRIBUTES =
      new SimpleTextAttributes(
          SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_BOLD, /*fgColor*/ null);

  @Getter @Setter private SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;

  @Override
  public String getValue() {
    return branch.getName();
  }

  @Override
  public boolean hasBulletPoint() {
    return true;
  }
}
