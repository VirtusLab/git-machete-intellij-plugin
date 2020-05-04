package com.virtuslab.gitmachete.frontend.ui.api.table;

import javax.swing.JComponent;

public interface IGraphTableManager {
  JComponent getGraphTable();

  void queueGraphTableRefreshOnDispatchThread();

  void queueRepositoryUpdateAndGraphTableRefresh();

  boolean isListingCommits();

  void setListingCommits(boolean isListingCommits);
}
