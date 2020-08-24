package com.virtuslab.gitmachete.frontend.ui.impl.cell;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.guieffect.qual.UIEffect;

@RequiredArgsConstructor
public class BranchOrCommitCellRenderer implements TableCellRenderer {
  private final boolean hasBranchActionToolTips;

  @Override
  @UIEffect
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
      int column) {
    return new BranchOrCommitCellRendererComponent(table, value, isSelected, hasFocus, row, column, hasBranchActionToolTips);
  }
}
