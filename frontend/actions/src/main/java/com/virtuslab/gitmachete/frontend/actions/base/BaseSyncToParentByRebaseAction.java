package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.util.Arrays;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnActionEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod({Arrays.class, GitMacheteBundle.class})
@CustomLog
public abstract class BaseSyncToParentByRebaseAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {
  private static final String NL = System.lineSeparator();

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val selectedGitRepo = getSelectedGitRepository(anActionEvent);
    val state = selectedGitRepo != null ? selectedGitRepo.getState() : null;
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU);

    if (state == null) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.repository.unknown-state"));

    } else if (state != Repository.State.NORMAL
        && !(isCalledFromContextMenu && state == Repository.State.DETACHED)) {

      val stateName = Match(state).of(
          Case($(Repository.State.GRAFTING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.cherry-pick")),
          Case($(Repository.State.DETACHED),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.detached-head")),
          Case($(Repository.State.MERGING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.merge")),
          Case($(Repository.State.REBASING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.rebase")),
          Case($(Repository.State.REVERTING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.revert")),
          Case($(), ": " + state.name().toLowerCase()));

      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.repository.status")
              .format(stateName));
    } else {

      val branchName = getNameOfBranchUnderAction(anActionEvent);
      val branch = getManagedBranchByName(anActionEvent, branchName);

      if (branch == null) {
        presentation.setEnabled(false);
        presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.undefined.machete-branch")
            .format("Rebase", getQuotedStringOrCurrent(branchName)));
      } else if (branch.isRoot()) {

        if (anActionEvent.getPlace().equals(ActionPlaces.TOOLBAR)) {
          presentation.setEnabled(false);
          presentation.setDescription(
              getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.root-branch")
                  .format(branch.getName()));
        } else { //contextmenu
          // in case of root branch we do not want to show this option at all
          presentation.setEnabledAndVisible(false);
        }

      } else if (branch.isNonRoot()) {
        val nonRootBranch = branch.asNonRoot();
        val upstream = nonRootBranch.getParent();
        presentation.setDescription(getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description")
            .format(branch.getName(), upstream.getName()));
      }

      val currentBranchNameIfManaged = getCurrentBranchNameIfManaged(anActionEvent);

      val isRebasingCurrent = branch != null && currentBranchNameIfManaged != null
          && currentBranchNameIfManaged.equals(branch.getName());
      if (isCalledFromContextMenu && isRebasingCurrent) {
        presentation.setText(getString("action.GitMachete.BaseSyncToParentByRebaseAction.text"));
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchName);

    if (branch != null) {
      if (branch.isNonRoot()) {
        doRebase(anActionEvent, branch.asNonRoot());
      } else {
        LOG.warn("Skipping the action because the branch '${branch.getName()}' is a root branch");
      }
    }
  }

  private void doRebase(AnActionEvent anActionEvent, INonRootManagedBranchSnapshot branchToRebase) {
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    val state = gitRepository != null ? gitRepository.getState() : null;
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU);
    val shouldExplicitlyCheckout = isCalledFromContextMenu
        && state != null && Repository.State.DETACHED == state;

    if (gitRepository != null && gitMacheteRepositorySnapshot != null) {
      new RebaseOnParentBackgroundable(project,
          getString("action.GitMachete.BaseSyncToParentByRebaseAction.hook.task-title"),
          gitRepository, gitMacheteRepositorySnapshot,
          branchToRebase,
          shouldExplicitlyCheckout).queue();
    }
  }

}
