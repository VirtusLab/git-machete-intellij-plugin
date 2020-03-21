package com.virtuslab.gitmachete.frontend.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import com.virtuslab.gitmachete.frontend.actions.RebaseCurrentBranchOntoParentAction;

public class GitMachetePanel {

  @Getter
  private final GitMacheteGraphTableManager gitMacheteGraphTableManager;

  public GitMachetePanel(Project project) {
    gitMacheteGraphTableManager = new GitMacheteGraphTableManager(project);
    gitMacheteGraphTableManager.updateAndRefreshInBackground();
  }

  public ActionToolbar createGitMacheteToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    DefaultActionGroup refresh = new DefaultActionGroup("Refresh", /* popup */ false);
    refresh.add(new RefreshGitMacheteStatusAction());

    DefaultActionGroup toggleListCommits = new DefaultActionGroup("Toggle List Commits", /* popup */ false);
    toggleListCommits.add(new ToggleListCommitsAction());

    DefaultActionGroup rebaseCurrentBranchOntoParent = new DefaultActionGroup("Rebase Current Branch Onto Parent",
        /* popup */ false);
    rebaseCurrentBranchOntoParent.add(new RebaseCurrentBranchOntoParentAction());

    gitMacheteActions.addAll(refresh, toggleListCommits, rebaseCurrentBranchOntoParent);

    ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar(GitMacheteContentProvider.GIT_MACHETE_TOOLBAR, gitMacheteActions, /* horizontal */ false);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGitMacheteGraphTable());
    return toolbar;
  }

  private class RefreshGitMacheteStatusAction extends AnAction {
    RefreshGitMacheteStatusAction() {
      super("Refresh Status", "Refresh status", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      gitMacheteGraphTableManager.updateAndRefreshInBackground();
    }
  }

  private class ToggleListCommitsAction extends ToggleAction implements DumbAware {
    ToggleListCommitsAction() {
      super("Toggle List Commits", "Toggle list commits", AllIcons.Actions.ShowHiddens);
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
