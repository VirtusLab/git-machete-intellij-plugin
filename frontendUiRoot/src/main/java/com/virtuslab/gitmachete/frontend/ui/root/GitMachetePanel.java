package com.virtuslab.gitmachete.frontend.ui.root;

import java.awt.BorderLayout;
import java.util.List;

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
import com.intellij.util.SmartList;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.common.value.qual.MinLen;

import com.virtuslab.gitmachete.frontend.actions.RebaseCurrentBranchOntoParentAction;
import com.virtuslab.gitmachete.frontend.actions.SlideOutCurrentBranchAction;
import com.virtuslab.gitmachete.frontend.keys.ActionIDs;
import com.virtuslab.gitmachete.frontend.ui.VcsRootComboBox;
import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTable;
import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTableManager;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public final class GitMachetePanel extends SimpleToolWindowPanel {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiRoot");

  public static final String GIT_MACHETE_TOOLBAR = "GitMacheteToolbar";
  private static final String REFRESH_STATUS_TEXT = "Refresh Status";
  private static final String REFRESH_STATUS_DESCRIPTION = "Refresh status";
  private static final String TOGGLE_LIST_COMMIT_TEXT = "Toggle List Commits";
  private static final String TOGGLE_LIST_COMMIT_DESCRIPTION = "Toggle list commits";

  private final GitMacheteGraphTableManager gitMacheteGraphTableManager;

  @UIEffect
  public GitMachetePanel(Project project) {
    super(/* vertical */ false, /* borderless */ true);
    LOG.debug("Instantiation of GitMachetePanel");

    // GitUtil.getRepositories(project) should never return empty list because it means there is no git repository in
    // an opened project, so Git Machete plugin shouldn't even be loaded in the first place
    @SuppressWarnings("value:assignment.type.incompatible")
    @MinLen(1)
    List<GitRepository> repositories = new SmartList<>(GitUtil.getRepositories(project));
    VcsRootComboBox vcsRootComboBox = new VcsRootComboBox(repositories);
    gitMacheteGraphTableManager = new GitMacheteGraphTableManager(project, vcsRootComboBox);
    gitMacheteGraphTableManager.updateAndRefreshInBackground();

    // This class is final, so the instance is `@Initialized` at this point.

    setToolbar(createGitMacheteVerticalToolbar().getComponent());
    add(VcsRootComboBox.createShrinkingWrapper(vcsRootComboBox), BorderLayout.NORTH);
    GitMacheteGraphTable gitMacheteGraphTable = gitMacheteGraphTableManager.getGitMacheteGraphTable();
    setContent(ScrollPaneFactory.createScrollPane(gitMacheteGraphTable));
  }

  @UIEffect
  private ActionToolbar createGitMacheteVerticalToolbar() {
    DefaultActionGroup gitMacheteActions = new DefaultActionGroup();

    // This check is needed as action register is shared between multiple running IDE instances
    // and we would not like to re-register the action.
    var refreshGitMacheteStatusAction = ActionManager.getInstance().getAction(ActionIDs.ACTION_REFRESH);
    if (refreshGitMacheteStatusAction == null) {
      refreshGitMacheteStatusAction = new RefreshGitMacheteStatusAction();
      ActionManager.getInstance().registerAction(ActionIDs.ACTION_REFRESH, refreshGitMacheteStatusAction);
    }

    gitMacheteActions.addAll(
        refreshGitMacheteStatusAction,
        new ToggleListCommitsAction(),
        new RebaseCurrentBranchOntoParentAction(),
        new SlideOutCurrentBranchAction());

    ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar(GIT_MACHETE_TOOLBAR, gitMacheteActions, /* horizontal */ false);
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
      LOG.debug("Refresh action invoked");
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
      LOG.debug(() -> "Commits visibility changing action triggered with state = ${state}");
      gitMacheteGraphTableManager.setListingCommits(state);
      gitMacheteGraphTableManager.refreshGraphTable();
    }
  }
}
