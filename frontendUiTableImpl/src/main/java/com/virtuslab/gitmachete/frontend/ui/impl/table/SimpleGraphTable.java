package com.virtuslab.gitmachete.frontend.ui.impl.table;

import javax.swing.ListSelectionModel;

import com.intellij.ui.ScrollingUtil;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.val;
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

  @UIEffect
  public static SimpleGraphTable deriveInstance(IGitMacheteRepositorySnapshot macheteRepositorySnapshot,
      boolean isListingCommitsEnabled, boolean shouldDisplayActionToolTips) {
    // We can keep the data - graph table model,
    // but wee need to reinstantiate the UI - demo graph table.
    return new SimpleGraphTable(deriveGraphTableModel(macheteRepositorySnapshot, isListingCommitsEnabled),
        shouldDisplayActionToolTips);
  }

  @UIEffect
  private static GraphTableModel deriveGraphTableModel(IGitMacheteRepositorySnapshot macheteRepositorySnapshot,
      boolean isListingCommitsEnabled) {
    val repositoryGraphCache = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphCache.class);
    val repositoryGraph = repositoryGraphCache.getRepositoryGraph(macheteRepositorySnapshot, isListingCommitsEnabled);
    return new GraphTableModel(repositoryGraph);
  }

  @UIEffect
  private SimpleGraphTable(GraphTableModel graphTableModel, boolean shouldDisplayActionToolTips) {
    super(graphTableModel);

    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);

    setCellSelectionEnabled(false);
    setColumnSelectionAllowed(false);
    setRowSelectionAllowed(true);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    setDefaultRenderer(BranchOrCommitCell.class, new BranchOrCommitCellRenderer(shouldDisplayActionToolTips));

    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);
  }
}
