package com.virtuslab.gitmachete.frontend.ui.impl.cell;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.checkerframework.checker.guieffect.qual.UIEffect;

public class BranchOrCommitCellRenderer implements TableCellRenderer {
  private final boolean hasBranchActionHints;

  public BranchOrCommitCellRenderer(boolean hasBranchActionHints) {
    this.hasBranchActionHints = hasBranchActionHints;
  }

  @Override
  @UIEffect
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
      int column) {
    return new BranchOrCommitCellRendererComponent(table, value, isSelected, hasFocus, row, column, hasBranchActionHints);
  }
}
