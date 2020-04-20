package com.virtuslab.gitmachete.frontend.ui.root;

import java.awt.BorderLayout;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.ScrollPaneFactory;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.common.value.qual.MinLen;

import com.virtuslab.gitmachete.frontend.actions.RebaseCurrentBranchOntoParentAction;
import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTable;
import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTableManager;

public final class GitMachetePanel extends SimpleToolWindowPanel {
  public static final String GIT_MACHETE_TOOLBAR = "GitMacheteToolbar";
  private static final String REFRESH_STATUS_TEXT = "Refresh Status";
  private static final String REFRESH_STATUS_DESCRIPTION = "Refresh status";
  private static final String TOGGLE_LIST_COMMIT_TEXT = "Toggle List Commits";
  private static final String TOGGLE_LIST_COMMIT_DESCRIPTION = "Toggle list commits";

  private final GitMacheteGraphTableManager gitMacheteGraphTableManager;
  private final VcsRootDropdown vcsRootDropdown;

  @UIEffect
  public GitMachetePanel(Project project) {
    super(/* vertical */ false, /* borderless */ true);

    // GitUtil.getRepositories(project) should never return empty list because it means there is no git repository in
    // opened project, so Git Machete plugin shouldn't even be loaded in the first place
    @SuppressWarnings("value:assignment.type.incompatible")
    @MinLen(1)
    List<GitRepository> repositories = List.ofAll(GitUtil.getRepositories(project));
    vcsRootDropdown = new VcsRootDropdown(repositories);
    gitMacheteGraphTableManager = new GitMacheteGraphTableManager(project, vcsRootDropdown);
    gitMacheteGraphTableManager.updateAndRefreshInBackground();

    // This class is final, so the instance is `@Initialized` at this point.

    addToolbarsToWindowPanel();
    GitMacheteGraphTable gitMacheteGraphTable = gitMacheteGraphTableManager.getGitMacheteGraphTable();
    setContent(ScrollPaneFactory.createScrollPane(gitMacheteGraphTable));
  }

  @UIEffect
  private void addToolbarsToWindowPanel() {
    setToolbar(createGitMacheteVerticalToolbar().getComponent());
    if (vcsRootDropdown.getRootCount() > 1) {
      add(createGitMacheteHorizontalToolbar().getComponent(), BorderLayout.NORTH);
    }
  }

  @UIEffect
  private ActionToolbar createGitMacheteVerticalToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    gitMacheteActions.addAll(
        new RefreshGitMacheteStatusAction(),
        new ToggleListCommitsAction(),
        new RebaseCurrentBranchOntoParentAction());

    ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar(GIT_MACHETE_TOOLBAR, gitMacheteActions, /* horizontal */ false);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGitMacheteGraphTable());
    return toolbar;
  }

  @UIEffect
  private ActionToolbar createGitMacheteHorizontalToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    gitMacheteActions.add(vcsRootDropdown);

    ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar(GIT_MACHETE_TOOLBAR, gitMacheteActions, /* horizontal */ true);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGitMacheteGraphTable());
    return toolbar;
  }

  private class RefreshGitMacheteStatusAction extends AnAction implements DumbAware {
    @UIEffect
    RefreshGitMacheteStatusAction() {
      super(REFRESH_STATUS_TEXT, REFRESH_STATUS_DESCRIPTION, AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      gitMacheteGraphTableManager.updateAndRefreshInBackground();
    }
  }

  private class ToggleListCommitsAction extends ToggleAction implements DumbAware {
    @UIEffect
    ToggleListCommitsAction() {
      super(TOGGLE_LIST_COMMIT_TEXT, TOGGLE_LIST_COMMIT_DESCRIPTION, AllIcons.Actions.ShowHiddens);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return gitMacheteGraphTableManager.isListingCommits();
    }

    @Override
    @UIEffect
    public void setSelected(AnActionEvent e, boolean state) {
      gitMacheteGraphTableManager.setListingCommits(state);
      gitMacheteGraphTableManager.refreshGraphTable();
    }
  }
}
