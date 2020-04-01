package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionIDs.ACTION_REBASE;
import static com.virtuslab.gitmachete.frontend.actions.DataKeys.KEY_GIT_MACHETE_REPOSITORY;
import static com.virtuslab.gitmachete.frontend.actions.DataKeys.KEY_SELECTED_BRANCH;

import java.util.Map;
import java.util.Optional;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.testFramework.MapDataContext;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 * </ul>
 */
public class RebaseCurrentBranchOntoParentAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(RebaseCurrentBranchOntoParentAction.class);

  public RebaseCurrentBranchOntoParentAction() {
    super("Rebase Current Branch Onto Parent", "Rebase current branch onto parent", AllIcons.Actions.Menu_cut);
  }

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    // TODO (#79): prohibit rebase during rebase/merge/revert etc.
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = anActionEvent.getData(KEY_GIT_MACHETE_REPOSITORY);
    assert gitMacheteRepository != null : "Can't get gitMacheteRepository";

    Optional<BaseGitMacheteBranch> branchToRebase = gitMacheteRepository.getCurrentBranchIfManaged();

    if (!branchToRebase.isPresent()) {
      LOG.error("There is no current branch managed by Git-Machete");
      return;
    }

    DataContext originalDataContext = anActionEvent.getDataContext();

    MapDataContext dataContext = new MapDataContext(
        Map.of(
            CommonDataKeys.PROJECT, originalDataContext.getData(CommonDataKeys.PROJECT),
            KEY_GIT_MACHETE_REPOSITORY, gitMacheteRepository,
            KEY_SELECTED_BRANCH, branchToRebase.get()));

    AnActionEvent actionEvent = new AnActionEvent(anActionEvent.getInputEvent(), dataContext, anActionEvent.getPlace(),
        anActionEvent.getPresentation(), anActionEvent.getActionManager(), anActionEvent.getModifiers());
    // Effectively delegating the action to RebaseSelectedBranchOntoParentAction (see the action id -> action class
    // binding in plugin.xml).
    ActionManager.getInstance().getAction(ACTION_REBASE).actionPerformed(actionEvent);
  }
}
