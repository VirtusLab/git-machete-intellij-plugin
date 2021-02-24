package com.virtuslab.gitmachete.frontend.ui.impl.table;

import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.ISimpleGraphTableProvider;

public class SimpleGraphTableProvider implements ISimpleGraphTableProvider {
  @Override
  @UIEffect
  public BaseGraphTable deriveInstance(IGitMacheteRepositorySnapshot macheteRepositorySnapshot,
      boolean isListingCommitsEnabled, boolean shouldDisplayActionToolTips) {
    // The reinstantiation is needed every time because without it
    // the table keeps the first IDE theme despite the theme changes.
    return SimpleGraphTable.deriveInstance(macheteRepositorySnapshot, isListingCommitsEnabled, shouldDisplayActionToolTips);
  }

  @Override
  @UIEffect
  public BaseGraphTable deriveDemoInstance() {
    return deriveInstance(new DemoGitMacheteRepositorySnapshot(), /* isListingCommitsEnabled */ true,
        /* shouldDisplayActionToolTips */ false);
  }
}
