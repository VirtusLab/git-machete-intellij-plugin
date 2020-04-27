package com.virtuslab.gitmachete.frontend.ui.api.table;

import javax.swing.JComponent;

import org.checkerframework.checker.guieffect.qual.UIEffect;

public interface IGraphTableManager {
  JComponent getGraphTable();

  @UIEffect
  void refreshGraphTable();

  void updateAndRefreshGraphTableInBackground();

  boolean isListingCommits();

  void setListingCommits(boolean isListingCommits);
}
