package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteRepository;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedBranchName;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.BasePullBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
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

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    Option<String> selectedBranchName = getSelectedBranchName(anActionEvent);

    if (selectedBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Pull disabled due to undefined current branch");
      return;
    }

    Option<SyncToRemoteStatus> syncToRemoteStatus = getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getBranchByName(selectedBranchName.get()))
        .map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Pull disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    boolean isEnabled = PULL_ENABLING_STATUSES.contains(relation);

    if (isEnabled) {
      Option<Boolean> isSelectedEqualCurrent = getGitMacheteRepository(anActionEvent)
          .flatMap(repo -> repo.getCurrentBranchIfManaged())
          .map(branch -> branch.getName())
          .map(branchName -> branchName.equals(selectedBranchName.get()));

      if (isSelectedEqualCurrent.isDefined() && isSelectedEqualCurrent.get()) {
        presentation.setText("Pull Current Branch");
      }

      presentation.setDescription("Pull branch '${selectedBranchName.get()}'");

    } else {
      presentation.setEnabled(false);
      String description = getRelationBaseDescription(relation);
      presentation.setDescription(description);
    }

  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    Project project = getProject(anActionEvent);
    Option<GitRepository> selectedVcsRepository = getSelectedVcsRepository(anActionEvent);
    Option<String> branchName = getSelectedBranchName(anActionEvent);

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
