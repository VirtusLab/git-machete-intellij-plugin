package com.virtuslab.gitmachete.frontend.ui.impl.table;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;

import com.intellij.ui.ScrollingUtil;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.NullGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphCache;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRenderer;

public final class SimpleGraphTable extends BaseGraphTable implements IGitMacheteRepositorySnapshotProvider {
  @Getter
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot = NullGitMacheteRepositorySnapshot.getInstance();

  private final TableCellRenderer tableCellRenderer;

  @UIEffect
  public static SimpleGraphTable deriveInstance(IGitMacheteRepositorySnapshot macheteRepositorySnapshot,
      boolean isListingCommitsEnabled, boolean hasBranchActionHints) {
    // We can keep the data - graph table model,
    // but wee need to reinstantiate the UI - demo graph table.
    return new SimpleGraphTable(deriveGraphTableModel(macheteRepositorySnapshot, isListingCommitsEnabled), hasBranchActionHints);
  }

  @UIEffect
  private static GraphTableModel deriveGraphTableModel(IGitMacheteRepositorySnapshot macheteRepositorySnapshot,
      boolean isListingCommitsEnabled) {
    var repositoryGraphCache = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphCache.class);
    var repositoryGraph = repositoryGraphCache.getRepositoryGraph(macheteRepositorySnapshot, isListingCommitsEnabled);
    return new GraphTableModel(repositoryGraph);
  }

  @UIEffect
  private SimpleGraphTable(GraphTableModel graphTableModel, boolean hasBranchActionHints) {
    super(graphTableModel);

    this.tableCellRenderer = new BranchOrCommitCellRenderer(hasBranchActionHints);

    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);

    setCellSelectionEnabled(false);
    setColumnSelectionAllowed(false);
    setRowSelectionAllowed(true);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    setDefaultRenderer(BranchOrCommitCell.class, tableCellRenderer);

    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);
  }
}
