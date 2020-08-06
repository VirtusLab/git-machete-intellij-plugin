package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.NullGitMacheteRepositorySnapshot;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public abstract class BaseGraphTable extends JBTable implements IGitMacheteRepositorySnapshotProvider {
  @UIEffect
  @Getter
  protected @Nullable IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  @UIEffect
  @SuppressWarnings("nullness") // to allow setAutoResizeMode
  protected BaseGraphTable(TableModel model) {
    super(model);

    setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
  }

  /**
   * This override of {@link JBTable#prepareRenderer} set column size to fit the content
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

  @Override
  @UIEffect
  public String getToolTipText(MouseEvent event) {
    Point point = event.getPoint();

    int rowIndex = rowAtPoint(point);
    int columnIndex = columnAtPoint(point);

    if (rowIndex != -1 && columnIndex != -1) {
      TableCellRenderer renderer = getCellRenderer(rowIndex, columnIndex);
      Component component = prepareRenderer(renderer, rowIndex, columnIndex);
      Rectangle cellRect = getCellRect(rowIndex, columnIndex, /* includeSpacing */ false);
      component.setBounds(cellRect);
      component.validate();
      component.doLayout();
      point.translate(-cellRect.x, -cellRect.y);
      Component comp = component.getComponentAt(point);
      if (comp instanceof JComponent) {
        return ((JComponent) comp).getToolTipText();
      }
    }

    // Fallback in case no subcomponent has tooltip text
    return getToolTipText();
  }

  @Override
  public void paint(@NotNull Graphics g) {

    super.paint(g);
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
