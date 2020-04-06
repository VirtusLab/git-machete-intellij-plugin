package com.virtuslab.gitmachete.frontend.ui;

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
import git4idea.GitUtil;
import io.vavr.collection.List;
import lombok.Getter;

import com.virtuslab.gitmachete.frontend.actions.RebaseCurrentBranchOntoParentAction;
import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTableManager;

public class GitMachetePanel {
  private static final String TOGGLE_LIST_COMMIT_TEXT = "Toggle List Commits";
  private static final String TOGGLE_LIST_COMMIT_DESCRIPTION = "Toggle list commits";
  private static final String REFRESH_STATUS_TEXT = "Refresh Status";
  private static final String REFRESH_STATUS_DESCRIPTION = "Refresh status";

  @Getter
  private final GitMacheteGraphTableManager gitMacheteGraphTableManager;
  private final VcsRootDropdown vcsRootDropdown;

  public GitMachetePanel(Project project) {
    vcsRootDropdown = new VcsRootDropdown(List.ofAll(GitUtil.getRepositories(project)));
    gitMacheteGraphTableManager = new GitMacheteGraphTableManager(project, vcsRootDropdown);
    gitMacheteGraphTableManager.updateAndRefreshInBackground();
  }

  public void addToolbarsToWindowPanel(SimpleToolWindowPanel windowPanel) {
    windowPanel.setToolbar(createGitMacheteVerticalToolbar().getComponent());
    if (vcsRootDropdown.getRootCount() > 1) {
      windowPanel.add(createGitMacheteHorizontalToolbar().getComponent(), BorderLayout.NORTH);
    }
  }

  private ActionToolbar createGitMacheteVerticalToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    gitMacheteActions.add(new RefreshGitMacheteStatusAction());
    gitMacheteActions.add(new ToggleListCommitsAction());
    gitMacheteActions.add(new RebaseCurrentBranchOntoParentAction());

    ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar(GitMacheteContentProvider.GIT_MACHETE_TOOLBAR, gitMacheteActions, /* horizontal */ false);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGitMacheteGraphTable());
    return toolbar;
  }

  private ActionToolbar createGitMacheteHorizontalToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    gitMacheteActions.add(vcsRootDropdown);

    ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar(GitMacheteContentProvider.GIT_MACHETE_TOOLBAR, gitMacheteActions, /* horizontal */ true);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGitMacheteGraphTable());
    return toolbar;
  }

  private class RefreshGitMacheteStatusAction extends AnAction {
    RefreshGitMacheteStatusAction() {
      super(REFRESH_STATUS_TEXT, REFRESH_STATUS_DESCRIPTION, AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      gitMacheteGraphTableManager.updateAndRefreshInBackground();
    }
  }

  private class ToggleListCommitsAction extends ToggleAction implements DumbAware {
    ToggleListCommitsAction() {
      super(TOGGLE_LIST_COMMIT_TEXT, TOGGLE_LIST_COMMIT_DESCRIPTION, AllIcons.Actions.ShowHiddens);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return gitMacheteGraphTableManager.isListingCommits();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      gitMacheteGraphTableManager.setListingCommits(state);
      gitMacheteGraphTableManager.refreshUI();
    }
  }
}
