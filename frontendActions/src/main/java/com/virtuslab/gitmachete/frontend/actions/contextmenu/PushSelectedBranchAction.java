package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentBranchNameIfManaged;
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
import com.virtuslab.gitmachete.frontend.actions.common.BasePushBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class PushSelectedBranchAction extends BasePushBranchAction {
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
      presentation.setDescription("Push disabled due to undefined current branch");
      return;
    }

    Option<SyncToRemoteStatus> syncToRemoteStatus = getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getBranchByName(selectedBranchName.get()))
        .map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Push disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    boolean isEnabled = PUSH_ELIGIBLE_STATUSES.contains(relation);

    if (isEnabled) {
      Option<Boolean> isSelectedEqualCurrent = getCurrentBranchNameIfManaged(anActionEvent)
          .map(branchName -> branchName.equals(selectedBranchName.get()));

      if (isSelectedEqualCurrent.isDefined() && isSelectedEqualCurrent.get()) {
        presentation.setText("Push Current Branch");
      }

      presentation.setDescription("Push branch '${selectedBranchName.get()}' using push dialog");

    } else {
      presentation.setEnabled(false);
      String description = getRelationBasedDescription(relation);
      presentation.setDescription(description);
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    Project project = getProject(anActionEvent);
    Option<GitRepository> selectedVcsRepository = getSelectedVcsRepository(anActionEvent);
    Option<String> branchName = getSelectedBranchName(anActionEvent);

    if (branchName.isDefined()) {
      if (selectedVcsRepository.isDefined()) {
        doPush(project, selectedVcsRepository.get(), branchName.get());
      } else {
        LOG.warn("Skipping the action because no VCS repository is selected");
      }
    } else {
      LOG.warn("Skipping the action because name of branch to push is undefined");
    }
  }
}
