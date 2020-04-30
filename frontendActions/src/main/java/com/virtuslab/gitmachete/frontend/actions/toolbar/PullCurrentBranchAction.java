package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.actions.common.BasePullBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class PullCurrentBranchAction extends BasePullBranchAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Option<BaseGitMacheteBranch> currentBranch = ActionUtils.getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getCurrentBranchIfManaged());

    Option<String> currentBranchName = currentBranch.map(branch -> branch.getName());

    if (currentBranchName.isEmpty()) {
      anActionEvent.getPresentation().setEnabled(false);
      anActionEvent.getPresentation().setDescription("Pull disabled due to undefined current branch");
      return;
    }

    Option<SyncToRemoteStatus> syncToRemoteStatus = currentBranch.map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      anActionEvent.getPresentation().setEnabled(false);
      anActionEvent.getPresentation().setDescription("Pull disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    boolean isEnabled = PULL_ENABLING_STATUSES.contains(relation);

    if (isEnabled) {
      anActionEvent.getPresentation().setDescription("Pull branch '${currentBranchName.get()}'");
    } else {
      anActionEvent.getPresentation().setEnabled(false);
      String description = getRelationBaseDescription(relation);
      anActionEvent.getPresentation().setDescription(description);
    }

  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    Project project = ActionUtils.getProject(anActionEvent);
    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(anActionEvent);
    Option<String> branchName = ActionUtils.getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getCurrentBranchIfManaged())
        .map(branch -> branch.getName());

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
