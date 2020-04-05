package com.virtuslab.gitmachete.frontend.ui.table;

import javax.swing.table.AbstractTableModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCell;

@AllArgsConstructor
public class GraphTableModel extends AbstractTableModel {
  static final int BRANCH_OR_COMMIT_COLUMN = 0;
  private static final int COLUMN_COUNT = BRANCH_OR_COMMIT_COLUMN + 1;
  private static final String[] COLUMN_NAMES = {"Branch or Commit value"};

  @Getter
  @Setter
  private RepositoryGraph repositoryGraph;

  @Override
  @NonNegative
  public int getRowCount() {
    return repositoryGraph.nodesCount();
  }

  @Override
  @NonNegative
  public final int getColumnCount() {
    return COLUMN_COUNT;
  }

  @Override
  public final Object getValueAt(int rowIndex, int columnIndex) {
    switch (columnIndex) {
      case BRANCH_OR_COMMIT_COLUMN :
        IGraphElement element = repositoryGraph.getGraphElement(rowIndex);
        return new BranchOrCommitCell(element, repositoryGraph.getPrintElements(rowIndex));
      default :
        throw new IllegalArgumentException(
            String.format("columnIndex is %d > %d", columnIndex, getColumnCount() - 1));
    }
  }

  @Override
  public Class<?> getColumnClass(int column) {
    switch (column) {
      case BRANCH_OR_COMMIT_COLUMN :
        return BranchOrCommitCell.class;
      default :
        throw new IllegalArgumentException(
            String.format("columnIndex is %d > %d", column, getColumnCount() - 1));
    }
  }

  @Override
  @SuppressWarnings({"index:array.access.unsafe.high", "index:array.access.unsafe.low"})
  public String getColumnName(int column) {
    return COLUMN_NAMES[column];
  }
}
