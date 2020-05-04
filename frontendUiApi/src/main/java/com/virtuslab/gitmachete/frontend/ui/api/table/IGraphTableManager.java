package com.virtuslab.gitmachete.frontend.ui.api.table;

import javax.swing.JComponent;

public interface IGraphTableManager {
  JComponent getGraphTable();

  void refreshGraphTable();

  void updateAndRefreshGraphTableInBackground();

  boolean isListingCommits();

  void setListingCommits(boolean isListingCommits);
}
