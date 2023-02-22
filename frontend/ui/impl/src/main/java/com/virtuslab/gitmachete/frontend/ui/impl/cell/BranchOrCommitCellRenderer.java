package com.virtuslab.gitmachete.frontend.ui.impl.cell;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.guieffect.qual.UIEffect;

@RequiredArgsConstructor
public class BranchOrCommitCellRenderer implements TableCellRenderer {
  private final boolean shouldDisplayActionToolTips;

  @Override
  @UIEffect
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
      int column) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    return new BranchOrCommitCellRendererComponent(table, value, isSelected, hasFocus, row, column,
        shouldDisplayActionToolTips);
  }
}
