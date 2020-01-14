package com.virtuslab.gitmachete.ui;

import static com.intellij.ui.IdeBorderFactory.createBorder;
import static com.intellij.util.ui.JBUI.Panels.simplePanel;
import static com.intellij.util.ui.UIUtil.addBorder;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.util.Alarm;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.virtuslab.gitmachete.backendroot.GitFactoryModule;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.IRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphImpl;
import com.virtuslab.gitmachete.graph.repositorygraph.data.RepositoryGraphFactory;
import com.virtuslab.gitmachete.ui.table.GitMacheteGraphTable;
import com.virtuslab.gitmachete.ui.table.GraphTableModel;
import java.awt.Dimension;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.swing.JComponent;

public class GitMacheteContentProvider implements ChangesViewContentProvider {
  private static final String GIT_MACHETE_TOOLBAR = "GitMacheteToolbar";
  private final Project project;
  private final GitMacheteGraphTable view;
  private boolean listCommits;

  private final Alarm tableUpdateAlarm;
  private final Object tableUpdateIndicatorLock = new Object();
  @Nonnull private ProgressIndicator tableUpdateIndicator = new EmptyProgressIndicator();

  private boolean disposed = false;

  public GitMacheteContentProvider(@Nonnull Project project) {
    IGitMacheteRepository repository = createRepository(project);
    this.listCommits = false;
    this.project = project;
    this.tableUpdateAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
    this.view = new GitMacheteGraphTable(new RepositoryGraphImpl(repository));
  }

  private IGitMacheteRepository createRepository(@Nonnull Project project) {
    IGitMacheteRepository repository = null;
    try {
      GitMacheteRepositoryFactory instance =
          GitFactoryModule.getInjector().getInstance(GitMacheteRepositoryFactory.class);
      repository =
          instance.create(
              Paths.get(Objects.requireNonNull(project.getBasePath())), Optional.empty());

    } catch (GitMacheteException e) {
      e.printStackTrace();
    }

    return repository;
  }

  @Override
  public JComponent initContent() {
    ActionToolbar gitMacheteToolbar = createGitMacheteToolbar();
    addBorder(gitMacheteToolbar.getComponent(), createBorder(JBColor.border(), SideBorder.RIGHT));
    BorderLayoutPanel gitMachetePanel =
        simplePanel(view).addToLeft(gitMacheteToolbar.getComponent());

    BorderLayoutPanel contentPanel =
        new BorderLayoutPanel() {
          @Override
          public Dimension getMinimumSize() {
            return isMinimumSizeSet()
                ? super.getMinimumSize()
                : gitMacheteToolbar.getComponent().getPreferredSize();
          }
        };
    contentPanel.addToCenter(gitMachetePanel);

    SimpleToolWindowPanel panel =
        new SimpleToolWindowPanel(false, true) {
          @Nonnull
          @Override
          public List<AnAction> getActions(boolean originalProvider) {
            return gitMacheteToolbar.getActions();
          }
        };
    panel.setContent(simplePanel(contentPanel));
    return panel;
  }

  @Override
  public void disposeContent() {
    disposed = true;
    tableUpdateAlarm.cancelAllRequests();

    synchronized (tableUpdateIndicatorLock) {
      tableUpdateIndicator.cancel();
    }
  }

  @Nonnull
  private ActionToolbar createGitMacheteToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    DefaultActionGroup refresh = new DefaultActionGroup("Refresh", false);
    refresh.getTemplatePresentation().setIcon(AllIcons.Actions.Refresh);
    refresh.add(new RefreshGitMacheteStatusAction());

    DefaultActionGroup listCommits = new DefaultActionGroup("List Commits", false);
    listCommits.getTemplatePresentation().setIcon(AllIcons.Actions.Show);
    listCommits.add(new ToggleListCommitsAction());

    gitMacheteActions.add(refresh);
    gitMacheteActions.add(listCommits);

    ActionToolbar toolbar =
        ActionManager.getInstance()
            .createActionToolbar(GIT_MACHETE_TOOLBAR, gitMacheteActions, false);
    toolbar.setTargetComponent(view);
    return toolbar;
  }

  private void refreshView() {
    ProgressIndicator indicator = new EmptyProgressIndicator();
    synchronized (tableUpdateIndicatorLock) {
      tableUpdateIndicator.cancel();
      tableUpdateIndicator = indicator;
    }

    ProgressManager.getInstance()
        .executeProcessUnderProgress(
            () -> {
              if (disposed
                  || !project.isInitialized()
                  || ApplicationManager.getApplication().isUnitTestMode()) return;
              if (!ProjectLevelVcsManager.getInstance(project).hasActiveVcss()) return;

              IGitMacheteRepository repository = createRepository(project);
              IRepositoryGraph repositoryGraph =
                  RepositoryGraphFactory.getRepositoryGraph(repository, listCommits);
              GraphTableModel graphTableModel = new GraphTableModel(repositoryGraph);

              GuiUtils.invokeLaterIfNeeded(
                  () -> {
                    if (disposed) return;
                    indicator.checkCanceled();
                    view.setModel(graphTableModel);
                  },
                  ModalityState.NON_MODAL);
            },
            indicator);
  }

  private class RefreshGitMacheteStatusAction extends AnAction {
    RefreshGitMacheteStatusAction() {
      super("Refresh Status", "Refresh status", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      refreshView();
    }
  }

  private class ToggleListCommitsAction extends ToggleAction implements DumbAware {
    ToggleListCommitsAction() {
      super("List Commits", "List commits", AllIcons.Actions.ShowHiddens);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return listCommits;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      listCommits = state;
      refreshView(); // todo: optimize by keeping repository
    }
  }
}
