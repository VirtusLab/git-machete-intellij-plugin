package com.virtuslab.gitmachete.frontend.ui.impl.table;

import javax.swing.table.AbstractTableModel;

import lombok.AllArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;

@AllArgsConstructor
public class GraphTableModel extends AbstractTableModel {
  private static final int BRANCH_OR_COMMIT_COLUMN = 0;
  private static final int COLUMN_COUNT = BRANCH_OR_COMMIT_COLUMN + 1;
  private static final String[] COLUMN_NAMES = {"Branch or Commit value"};

  private final IRepositoryGraph repositoryGraph;

  @Override
  public @NonNegative int getRowCount() {
    return repositoryGraph.getNodesCount();
  }

  @Override
  public @NonNegative int getColumnCount() {
    return COLUMN_COUNT;
  }

  @Override
  public Object getValueAt(@NonNegative int rowIndex, @NonNegative int columnIndex) {
    if (columnIndex == BRANCH_OR_COMMIT_COLUMN) {
      IGraphItem graphItem = repositoryGraph.getGraphItem(rowIndex);
      return new BranchOrCommitCell(graphItem, repositoryGraph.getRenderParts(rowIndex));
    }
    throw new IllegalArgumentException("columnIndex is ${columnIndex} > ${getColumnCount() - 1}");
  }

  @Override
  public Class<?> getColumnClass(int column) {
    if (column == BRANCH_OR_COMMIT_COLUMN) {
      return BranchOrCommitCell.class;
    }
    throw new IllegalArgumentException("columnIndex is ${column} > ${getColumnCount() - 1}");
  }

  @Override
  @SuppressWarnings({"index:array.access.unsafe.high", "index:array.access.unsafe.low"})
  public String getColumnName(int column) {
    return COLUMN_NAMES[column];
  }
}
