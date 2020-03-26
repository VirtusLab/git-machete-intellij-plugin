package com.virtuslab.gitmachete.frontend.ui.table;

import java.text.MessageFormat;

import javax.swing.table.AbstractTableModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCell;

@AllArgsConstructor
public class GraphTableModel extends AbstractTableModel {
  static final int BRANCH_OR_COMMIT_COLUMN = 0;
  private static final int COLUMN_COUNT = BRANCH_OR_COMMIT_COLUMN + 1;
  private static final String[] COLUMN_NAMES = {"Branch or Commit value"};
  private static final String ILLEGAL_ARGUMENT_ERROR_MESSAGE = "columnIndex is {0} > {1}";

  @Getter
  @Setter
  private RepositoryGraph repositoryGraph;

  @Override
  public int getRowCount() {
    return repositoryGraph.nodesCount();
  }

  @Override
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
            MessageFormat.format(ILLEGAL_ARGUMENT_ERROR_MESSAGE, columnIndex, getColumnCount() - 1));
    }
  }

  @Override
  public Class<?> getColumnClass(int column) {
    switch (column) {
      case BRANCH_OR_COMMIT_COLUMN :
        return BranchOrCommitCell.class;
      default :
        throw new IllegalArgumentException(
            MessageFormat.format(ILLEGAL_ARGUMENT_ERROR_MESSAGE, column, getColumnCount() - 1));
    }
  }

  @Override
  public String getColumnName(int column) {
    return COLUMN_NAMES[column];
  }
}
