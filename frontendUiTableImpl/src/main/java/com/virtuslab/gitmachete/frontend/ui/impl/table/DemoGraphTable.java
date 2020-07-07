package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphCache;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRendererComponent;

public final class DemoGraphTable extends JBTable {

  private static final GraphTableModel GRAPH_TABLE_MODEL = deriveGraphTableModel();

  @UIEffect
  public static DemoGraphTable deriveInstance() {
    return new DemoGraphTable(GRAPH_TABLE_MODEL);
  }

  @UIEffect
  private static GraphTableModel deriveGraphTableModel() {
    var repositoryGraphCache = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphCache.class);
    var gitMacheteRepositorySnapshot = new DemoGitMacheteRepositorySnapshot();
    var repositoryGraph = repositoryGraphCache.getRepositoryGraph(gitMacheteRepositorySnapshot, /* isListingCommits */ true);
    return new GraphTableModel(repositoryGraph);
  }

  @UIEffect
  private DemoGraphTable(GraphTableModel graphTableModel) {
    super(graphTableModel);

    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);

    setDefaultRenderer(BranchOrCommitCell.class, BranchOrCommitCellRendererComponent::new);

    setCellSelectionEnabled(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);
  }
}
