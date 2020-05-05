package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.actions.common.BasePushBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class PushSelectedBranchAction extends BasePushBranchAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Option<String> selectedBranchName = ActionUtils.getSelectedBranchName(anActionEvent);

    if (selectedBranchName.isDefined()) {
      Option<SyncToRemoteStatus> syncToRemoteStatus = ActionUtils.getGitMacheteRepository(anActionEvent)
          .flatMap(repo -> repo.getBranchByName(selectedBranchName.get()))
          .flatMap(branch -> Option.of(branch.getSyncToRemoteStatus()));

      if (syncToRemoteStatus.isDefined()) {
        SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
        boolean isEnabled = PUSH_ENABLING_STATUSES.contains(relation);

        if (isEnabled) {
          Option<Boolean> isSelectedEqualCurrent = ActionUtils.getGitMacheteRepository(anActionEvent)
              .flatMap(repo -> Option.of(repo.getCurrentBranchIfManaged()))
              .flatMap(branch -> branch.isDefined() ? Option.of(branch.get().getName()) : Option.none())
              .flatMap(branchName -> Option.of(branchName.equals(selectedBranchName.get())));

          if (isSelectedEqualCurrent.isDefined() && isSelectedEqualCurrent.get()) {
            anActionEvent.getPresentation().setText("Push Current Branch");
          }
          anActionEvent.getPresentation().setDescription("Push branch '${selectedBranchName.get()}' with push dialog");
        } else {
          anActionEvent.getPresentation().setEnabled(false);
          String descriptionSpec = Match(relation).of(
              Case($(SyncToRemoteStatus.Relation.Behind), "behind its remote"),
              Case($(SyncToRemoteStatus.Relation.InSync), "in sync to its remote"),
              Case($(), "in unknown status '${relation.toString()}' to its remote"));
          anActionEvent.getPresentation().setDescription("Push disabled because current branch is ${descriptionSpec}");
        }

      } else {
        anActionEvent.getPresentation().setEnabled(false);
        anActionEvent.getPresentation().setDescription("Push disabled due to undefined sync to remote status");
      }

    } else {
      anActionEvent.getPresentation().setEnabled(false);
      anActionEvent.getPresentation().setDescription("Push disabled due to undefined current branch");
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    Project project = ActionUtils.getProject(anActionEvent);

    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(anActionEvent);

    Option<String> branchName = ActionUtils.getSelectedBranchName(anActionEvent);

    if (branchName.isDefined()) {
      doPush(project, selectedVcsRepository.toJavaList(), branchName.get());
    } else {
      LOG.warn("Skipping the action because name of branch to push is undefined");
    }
  }
}
