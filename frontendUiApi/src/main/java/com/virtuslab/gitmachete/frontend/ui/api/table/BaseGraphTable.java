package com.virtuslab.gitmachete.frontend.ui.api.table;

import javax.swing.table.AbstractTableModel;

import com.intellij.ui.table.JBTable;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public abstract class BaseGraphTable extends JBTable {

  @UIEffect
  protected BaseGraphTable(AbstractTableModel model) {
    super(model);
  }

  @UIEffect
  public abstract boolean isListingCommits();

  @UIEffect
  public abstract void setListingCommits(boolean isListingCommits);

  /**
   * Refresh the model synchronously (i.e. in a blocking manner).
   * Must be called from UI thread (hence {@link UIEffect}).
   */
  @UIEffect
  public abstract void refreshModel();

  public final void queueRepositoryUpdateAndModelRefresh() {
    queueRepositoryUpdateAndModelRefresh(() -> {});
  }

  /**
   * Queues repository update as a background task, which in turn itself queues model refresh onto the UI thread.
   * As opposed to {@link BaseGraphTable#refreshModel}, does not need to be called from UI thread (i.e. is not {@link UIEffect}).
   *
   * @param doOnUIThreadWhenDone action to be executed on UI thread after model refresh is complete
   */
  public abstract void queueRepositoryUpdateAndModelRefresh(@UI Runnable doOnUIThreadWhenDone);
}
