package com.virtuslab.gitmachete.frontend.ui.api.table;

import java.awt.Component;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BaseGraphTable extends JBTable {
  @UIEffect
  @SuppressWarnings("nullness:method.invocation.invalid") // to allow for setAutoResizeMode despite the object isn't initialized yet
  protected BaseGraphTable(TableModel model) {
    super(model);

    // Without set autoresize off column will expand to whole table width (will not fit the content size)
    setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
  }

  /**
   * This method that overrides {@link JBTable#prepareRenderer} is responsible for setting column size that fits the content
   */
  @Override
  @UIEffect
  public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
    Component component = super.prepareRenderer(renderer, row, column);
    int rendererWidth = component.getPreferredSize().width;
    TableColumn tableColumn = getColumnModel().getColumn(column);
    tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
    return component;
  }

  @UIEffect
  public void setTextForEmptyTable(String upperText, @Nullable String lowerText, @Nullable @UI Runnable onClickRunnable) {
    var attrs = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.linkColor());
    var statusText = getEmptyText().setText(upperText);
    if (lowerText != null) {
      statusText.appendSecondaryText(lowerText, attrs,
          /* listener */ onClickRunnable != null ? __ -> onClickRunnable.run() : null);
    }
  }

  @UIEffect
  public void setTextForEmptyTable(String upperText) {
    setTextForEmptyTable(upperText, /* lowerText */ null, /* onClickRunnable */ null);
  }
}
