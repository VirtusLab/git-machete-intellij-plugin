package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionIDs.ACTION_REBASE;
import static com.virtuslab.gitmachete.frontend.actions.DataKeyIDs.KEY_SELECTED_BRANCH;
import static com.virtuslab.gitmachete.frontend.actions.DataKeyIDs.KEY_TABLE_MANAGER;

import java.util.Map;
import java.util.Optional;

import io.vavr.control.Try;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.testFramework.MapDataContext;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.ui.GitMacheteGraphTableManager;

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
    GitMacheteGraphTableManager tableManager = anActionEvent.getData(KEY_TABLE_MANAGER);
    assert tableManager != null : "Can't get table manager";

    IGitMacheteRepository gitMacheteRepository = tableManager.getRepository();
    assert gitMacheteRepository != null : "Can't get gitMacheteRepository";

    @SuppressWarnings("methodref.return.invalid")
    Optional<IGitMacheteBranch> branchToRebase = Try.of(gitMacheteRepository::getCurrentBranchIfManaged)
        .onFailure(e -> LOG.error("Exception occurred while getting current branch")).get();

    if (branchToRebase.isEmpty()) {
      LOG.error("There is no current branch managed by Git-Machete");
      return;
    }

    DataContext originalDataContext = anActionEvent.getDataContext();

    MapDataContext dataContext = new MapDataContext(
        Map.of(
            CommonDataKeys.PROJECT, originalDataContext.getData(CommonDataKeys.PROJECT),
            KEY_TABLE_MANAGER, originalDataContext.getData(KEY_TABLE_MANAGER),
            KEY_SELECTED_BRANCH, branchToRebase.get()));

    AnActionEvent actionEvent = new AnActionEvent(anActionEvent.getInputEvent(), dataContext, anActionEvent.getPlace(),
        anActionEvent.getPresentation(), anActionEvent.getActionManager(), anActionEvent.getModifiers());
    ActionManager.getInstance().getAction(ACTION_REBASE).actionPerformed(actionEvent);
  }
}
