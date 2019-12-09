package com.virtuslab.gitmachete.ui.table;

import com.virtuslab.gitmachete.graph.model.GraphElement;
import com.virtuslab.gitmachete.graph.repositoryGraph.IRepositoryGraph;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCell;
import javax.annotation.Nonnull;
import javax.swing.table.AbstractTableModel;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GraphTableModel extends AbstractTableModel {
  private static final int BRANCH_OR_COMMIT_COLUMN = 0;
  private static final int COLUMN_COUNT = BRANCH_OR_COMMIT_COLUMN + 1;
  private static final String[] COLUMN_NAMES = {"Branch or Commit value"};

  @Nonnull private final IRepositoryGraph iRepositoryGraph;

  @Override
  public int getRowCount() {
    return iRepositoryGraph.nodesCount();
  }

  @Override
  public final int getColumnCount() {
    return COLUMN_COUNT;
  }

  @Nonnull
  @Override
  public final Object getValueAt(int rowIndex, int columnIndex) {
    switch (columnIndex) {
      case BRANCH_OR_COMMIT_COLUMN:
        GraphElement element = iRepositoryGraph.getGraphElement(rowIndex);
        return new BranchOrCommitCell(element, iRepositoryGraph.getPrintElements(rowIndex));
      default:
        throw new IllegalArgumentException(
            "columnIndex is " + columnIndex + " > " + (getColumnCount() - 1));
    }
  }

  @Override
  public Class<?> getColumnClass(int column) {
    switch (column) {
      case BRANCH_OR_COMMIT_COLUMN:
        return BranchOrCommitCell.class;
      default:
        throw new IllegalArgumentException(
            "columnIndex is " + column + " > " + (getColumnCount() - 1));
    }
  }

  @Override
  public String getColumnName(int column) {
    return COLUMN_NAMES[column];
  }
}
