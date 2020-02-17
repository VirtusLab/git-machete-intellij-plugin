package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.awt.Color;
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

  private static final JBColor branchTextColor = new JBColor(Color.BLACK, Color.WHITE);

  public static final SimpleTextAttributes UNDERLINE_BOLD_ATTRIBUTES =
      new SimpleTextAttributes(
          SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_BOLD, branchTextColor);

  @Getter @Setter
  private SimpleTextAttributes attributes =
      new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, branchTextColor);

  @Override
  public String getValue() {
    return branch.getName();
  }

  @Override
  public boolean hasBulletPoint() {
    return true;
  }
}
