package com.virtuslab.gitmachete.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.virtuslab.gitmachete.actions.GitInteractiveRebaseAction;
import javax.annotation.Nonnull;
import lombok.Getter;

public class GitMachetePanel {

  @Getter private final GitMacheteGraphTableManager gitMacheteGraphTableManager;

  public GitMachetePanel(@Nonnull Project project) {
    gitMacheteGraphTableManager = new GitMacheteGraphTableManager(project);
    gitMacheteGraphTableManager.updateRepository();
    gitMacheteGraphTableManager.refreshUI();
  }

  @Nonnull
  public ActionToolbar createGitMacheteToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    DefaultActionGroup refresh = new DefaultActionGroup("Refresh", /*popup*/ false);
    refresh.add(new RefreshGitMacheteStatusAction());

    DefaultActionGroup toggleListCommits =
        new DefaultActionGroup("Toggle List Commits", /*popup*/ false);
    toggleListCommits.add(new ToggleListCommitsAction());

    DefaultActionGroup updateCurrentBranch =
        new DefaultActionGroup("Update Current Branch", /*popup*/ false);
    updateCurrentBranch.add(new GitInteractiveRebaseAction(gitMacheteGraphTableManager));

    gitMacheteActions.add(refresh);
    gitMacheteActions.add(toggleListCommits);
    gitMacheteActions.add(updateCurrentBranch);

    ActionToolbar toolbar =
        ActionManager.getInstance()
            .createActionToolbar(
                GitMacheteContentProvider.GIT_MACHETE_TOOLBAR,
                gitMacheteActions,
                /*horizontal*/ false);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGitMacheteGraphTable());
    return toolbar;
  }

  private class RefreshGitMacheteStatusAction extends AnAction {
    RefreshGitMacheteStatusAction() {
      super("Refresh Status", "Refresh status", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      gitMacheteGraphTableManager.updateRepository();
      gitMacheteGraphTableManager.refreshUI();
    }
  }

  private class ToggleListCommitsAction extends ToggleAction implements DumbAware {
    ToggleListCommitsAction() {
      super("Toggle List Commits", "Toggle list commits", AllIcons.Actions.ShowHiddens);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return gitMacheteGraphTableManager.isListingCommits();
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      gitMacheteGraphTableManager.setListingCommits(state);
      gitMacheteGraphTableManager.refreshUI();
    }
  }
}
