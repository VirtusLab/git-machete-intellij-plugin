package com.virtuslab.gitmachete.frontend.ui.api.table;

import java.nio.file.Path;

import javax.swing.table.AbstractTableModel;

import com.intellij.ui.table.JBTable;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public abstract class BaseGraphTable extends JBTable {

  @UIEffect
  protected BaseGraphTable(AbstractTableModel model) {
    super(model);
  }

  @UIEffect
  public abstract boolean isListingCommits();

  @UIEffect
  public abstract void setListingCommits(boolean isListingCommits);

  @UIEffect
  public abstract void refreshModel();

  @UIEffect
  public abstract void refreshModel(@Nullable IGitMacheteRepository gitMacheteRepository, Path macheteFilePath,
      boolean isMacheteFilePresent);

  /**
   * Queues repository update as a background task, which in turn itself queues model refresh onto the UI thread.
   * As opposed to {@link BaseGraphTable#refreshModel}, does not need to be called from UI thread (i.e. is not {@link UIEffect}).
   */
  public abstract void queueRepositoryUpdateAndModelRefresh();
}
