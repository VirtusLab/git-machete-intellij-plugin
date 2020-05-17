package com.virtuslab.gitmachete.frontend.actions.toolbar;

import javax.swing.JComponent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColorToJBColorMapper;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainterFactory;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRenderer;
import com.virtuslab.gitmachete.frontend.ui.impl.table.GraphTableModel;

/**
 * Expects DataKeys:
 */
public class HelpAction extends DumbAwareAction {
  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent e) {
    new HelpDialog().show();
  }

  @UI
  private final class HelpDialog extends DialogWrapper {
    private static final int CENTER_PANEL_HEIGHT = 250;
    private static final int CENTER_PANEL_WIDTH = 800;

    protected HelpDialog() {
      super(/* canBeParent */ false);
      init();
      setTitle("Git Machete Help");
    }

    @Override
    protected JComponent createCenterPanel() {
      var panel = JBUI.Panels.simplePanel(0, 2);

      var iRepositoryGraphFactory = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphFactory.class);
      var gitMacheteRepository = new HelpGitMacheteRepository();
      var repositoryGraph = iRepositoryGraphFactory.getRepositoryGraph(gitMacheteRepository, /* isListingCommits */ true);
      var graphTableModel = new GraphTableModel(repositoryGraph);
      var helpGraphTable = new HelpGraphTable(graphTableModel);
      panel.addToCenter(ScrollPaneFactory.createScrollPane(helpGraphTable));
      panel.setPreferredSize(new JBDimension(CENTER_PANEL_WIDTH, CENTER_PANEL_HEIGHT));
      return panel;
    }
  }

  private final class HelpGraphTable extends JBTable {
    @UIEffect
    HelpGraphTable(GraphTableModel graphTableModel) {
      super(graphTableModel);

      var graphCellPainterFactory = RuntimeBinding.instantiateSoleImplementingClass(IGraphCellPainterFactory.class);
      var graphCellPainter = graphCellPainterFactory.create(/* colorProvider */ GraphItemColorToJBColorMapper::getColor,
          /* table */ this);

      createDefaultColumnsFromModel();

      // Otherwise sizes would be recalculated after each TableColumn re-initialization
      setAutoCreateColumnsFromModel(false);

      @SuppressWarnings("guieffect:assignment.type.incompatible")
      @AlwaysSafe
      BranchOrCommitCellRenderer branchOrCommitCellRenderer = new BranchOrCommitCellRenderer(/* table */ this,
          graphCellPainter);
      setDefaultRenderer(BranchOrCommitCell.class, branchOrCommitCellRenderer);

      setCellSelectionEnabled(false);
      setShowVerticalLines(false);
      setShowHorizontalLines(false);
      setIntercellSpacing(JBUI.emptySize());
      setTableHeader(new InvisibleResizableHeader());

      getColumnModel().setColumnSelectionAllowed(false);

      ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);
    }
  }
}
