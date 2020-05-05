package com.virtuslab.gitmachete.frontend.ui.api.table;

import org.checkerframework.checker.guieffect.qual.UIEffect;

public interface IGraphTable {
  @UIEffect
  boolean isListingCommits();

  @UIEffect
  void setListingCommits(boolean isListingCommits);

  @UIEffect
  void refreshModel();
}
