package com.virtuslab.gitmachete.ui.table;

import com.virtuslab.gitmachete.api.IRepositoryGraph;
import com.virtuslab.gitmachete.ui.render.GraphBranchCell;
import javax.annotation.Nonnull;
import javax.swing.table.AbstractTableModel;

public class GraphTableModel extends AbstractTableModel {
  private static final int BRANCH_COLUMN = 0;
  private static final int COLUMN_COUNT = BRANCH_COLUMN + 1;
  private static final String[] COLUMN_NAMES = {"Branch name"};

  @Nonnull private final IRepositoryGraph myIRepositoryGraph;

  GraphTableModel(@Nonnull IRepositoryGraph IRepositoryGraph) {
    myIRepositoryGraph = IRepositoryGraph;
  }

  @Override
  public int getRowCount() {
    return myIRepositoryGraph.nodesCount();
  }

  @Override
  public final int getColumnCount() {
    return COLUMN_COUNT;
  }

  @Nonnull
  @Override
  public final Object getValueAt(int rowIndex, int columnIndex) {
    switch (columnIndex) {
      case BRANCH_COLUMN:
        String branchName = myIRepositoryGraph.getBranch(rowIndex).getName();
        return new GraphBranchCell(branchName, myIRepositoryGraph.getPrintElements(rowIndex));
      default:
        throw new IllegalArgumentException(
            "columnIndex is " + columnIndex + " > " + (getColumnCount() - 1));
    }
  }

  @Override
  public Class<?> getColumnClass(int column) {
    switch (column) {
      case BRANCH_COLUMN:
        return GraphBranchCell.class;
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
