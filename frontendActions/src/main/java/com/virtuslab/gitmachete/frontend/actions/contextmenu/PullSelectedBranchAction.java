package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.actions.common.BasePullBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class PullSelectedBranchAction extends BasePullBranchAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Option<String> selectedBranchName = ActionUtils.getSelectedBranchName(anActionEvent);

    if (selectedBranchName.isEmpty()) {
      anActionEvent.getPresentation().setEnabled(false);
      anActionEvent.getPresentation().setDescription("Pull disabled due to undefined current branch");
      return;
    }

    Option<SyncToRemoteStatus> syncToRemoteStatus = ActionUtils.getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getBranchByName(selectedBranchName.get()))
        .map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      anActionEvent.getPresentation().setEnabled(false);
      anActionEvent.getPresentation().setDescription("Pull disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    boolean isEnabled = PULL_ENABLING_STATUSES.contains(relation);

    if (isEnabled) {
      Option<Boolean> isSelectedEqualCurrent = ActionUtils.getGitMacheteRepository(anActionEvent)
          .flatMap(repo -> repo.getCurrentBranchIfManaged())
          .map(branch -> branch.getName())
          .map(branchName -> branchName.equals(selectedBranchName.get()));

      if (isSelectedEqualCurrent.isDefined() && isSelectedEqualCurrent.get()) {
        anActionEvent.getPresentation().setText("Pull Current Branch");
      }
      anActionEvent.getPresentation().setDescription("Pull branch '${selectedBranchName.get()}'");
    } else {
      anActionEvent.getPresentation().setEnabled(false);
      String description = getRelationBaseDescription(relation);
      anActionEvent.getPresentation().setDescription(description);
    }

  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    Project project = ActionUtils.getProject(anActionEvent);
    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(anActionEvent);
    Option<String> branchName = ActionUtils.getSelectedBranchName(anActionEvent);

    if (branchName.isDefined()) {
      if (selectedVcsRepository.isDefined()) {
        doPull(project, selectedVcsRepository.get(), branchName.get());
      } else {
        LOG.warn("Skipping the action because no VCS repository is selected");
      }
    } else {
      LOG.warn("Skipping the action because name of branch to pull is undefined");
    }
  }
}
