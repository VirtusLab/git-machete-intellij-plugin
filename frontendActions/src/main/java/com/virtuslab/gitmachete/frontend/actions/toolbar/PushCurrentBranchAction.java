package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 * </ul>
 */
public class PushCurrentBranchAction extends DumbAwareAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  private final List<SyncToRemoteStatus.Relation> PUSH_ENABLING_STATUSES = List.of(
      SyncToRemoteStatus.Relation.Ahead,
      SyncToRemoteStatus.Relation.Diverged,
      SyncToRemoteStatus.Relation.Untracked);

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Option<String> currentBranchName = ActionUtils.getSelectedVcsRepository(anActionEvent)
        .flatMap(ideaRepo -> Option.of(ideaRepo.getCurrentBranch()))
        .flatMap(ideaBranch -> Option.of(ideaBranch.getName()));

    if (currentBranchName.isDefined()) {
      Option<SyncToRemoteStatus> syncToRemoteStatus = ActionUtils.getGitMacheteRepository(anActionEvent)
          .flatMap(repo -> repo.getBranchByName(currentBranchName.get()))
          .flatMap(branch -> Option.of(branch.getSyncToRemoteStatus()));

      if (syncToRemoteStatus.isDefined()) {
        SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
        boolean isDisabled = !PUSH_ENABLING_STATUSES.contains(relation);

        if (isDisabled) {
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
    if (selectedVcsRepository.isDefined()) {
      new VcsPushDialog(project, selectedVcsRepository.toJavaList(), /* currentRepo */ null).show();
    } else {
      LOG.warn("Skipping the action because no VCS repository is selected");
    }
  }
}
