package com.virtuslab.gitmachete.frontend.ui.api.table;

import java.nio.file.Path;

import javax.swing.table.AbstractTableModel;

import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

/**
 *  This class compared to SimpleGraphTable has graph table refreshing
 */

public abstract class BaseEnhancedGraphTable extends BaseGraphTable {
  @UIEffect
  protected BaseEnhancedGraphTable(AbstractTableModel model) {
    super(model);
  }

  @UIEffect
  public abstract void setListingCommits(boolean isListingCommits);

  /**
   * Refresh the model synchronously (i.e. in a blocking manner).
   * Must be called from the UI thread (hence {@link UIEffect}).
   */
  @UIEffect
  public abstract void refreshModel();

  @UIEffect
  public abstract void queueDiscover(Path macheteFilePath, @UI Runnable doOnUIThreadWhenReady);

  /**
   * Queues repository update as a background task, which in turn itself queues model refresh onto the UI thread.
   * As opposed to {@link BaseEnhancedGraphTable#refreshModel}, does not need to be called from the UI thread (i.e. is not {@link UIEffect}).
   *
   * @param doOnUIThreadWhenReady an action to execute on the UI thread after the model is refreshed.
   */
  public abstract void queueRepositoryUpdateAndModelRefresh(@UI Runnable doOnUIThreadWhenReady);

  public final void queueRepositoryUpdateAndModelRefresh() {
    queueRepositoryUpdateAndModelRefresh(() -> {});
  }
}
