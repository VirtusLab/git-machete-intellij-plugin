package com.virtuslab.gitmachete.frontend.actions.base;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseFastForwardParentToMatchBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyProject {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    if (branchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation
          .setDescription(GitMacheteBundle.message("action.description.disabled.undefined.branch-name", "Fast forward"));
      return;
    }

    var gitMacheteBranch = getGitMacheteBranchByName(anActionEvent, branchName.get());
    if (gitMacheteBranch.isEmpty()) {
      presentation.setEnabled(false);
      presentation
          .setDescription(GitMacheteBundle.message("action.description.disabled.undefined.machete-branch", "Fast forward"));
      return;
    }

    if (gitMacheteBranch.get().isRootBranch()) {
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription(GitMacheteBundle.message("action.fast-forward.description.root.branch", branchName.get()));
      } else { // contextmenu
        // in case of root branch we do not want to show this option at all
        presentation.setEnabledAndVisible(false);
      }
      return;
    }

    var gitMacheteNonRoot = gitMacheteBranch.get().asNonRootBranch();
    var syncToParentStatus = gitMacheteNonRoot.getSyncToParentStatus();

    if (syncToParentStatus == SyncToParentStatus.InSync) {

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(GitMacheteBundle.message("action.fast-forward.text.current-branch"));
      }

      var parentName = gitMacheteNonRoot.getUpstreamBranch().getName();
      presentation.setDescription(GitMacheteBundle.message("action.fast-forward.description", parentName, branchName.get()));

    } else {
      presentation.setEnabled(false);
      var desc = Match(syncToParentStatus).of(
          Case($(SyncToParentStatus.InSyncButForkPointOff),
              GitMacheteBundle.message("sync-to-parent-status.in-sync-but-fork-point-off")),
          Case($(SyncToParentStatus.MergedToParent), GitMacheteBundle.message("sync-to-parent-status.merged-to-parent")),
          Case($(SyncToParentStatus.OutOfSync), GitMacheteBundle.message("sync-to-parent-status.out-of-sync")),
          Case($(), GitMacheteBundle.message("sync-to-parent-status.unknown", syncToParentStatus.toString())));

      presentation
          .setDescription(GitMacheteBundle.message("action.description.disabled.branch.status", "Fast forward", desc));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var gitMacheteBranch = getNameOfBranchUnderAction(anActionEvent).flatMap(b -> getGitMacheteBranchByName(anActionEvent, b));

    if (gitMacheteBranch.isDefined() && gitRepository.isDefined()) {
      assert gitMacheteBranch.get().isNonRootBranch() : "Provided machete branch to fast forward is a root";
      doFastForward(project, gitRepository.get(), gitMacheteBranch.get().asNonRootBranch());
    }
  }

  private void doFastForward(Project project,
      GitRepository gitRepository,
      IGitMacheteNonRootBranch gitMacheteNonRootBranch) {
    var trackingInfo = gitRepository.getBranchTrackInfo(gitMacheteNonRootBranch.getName());
    var parentTrackingInfo = gitRepository.getBranchTrackInfo(gitMacheteNonRootBranch.getUpstreamBranch().getName());

    if (trackingInfo == null) {
      log().warn("No branch tracking info for branch ${gitMacheteNonRootBranch.getName()}");
      return;
    } else if (parentTrackingInfo == null) {
      log().warn("No branch tracking info for parent branch ${gitMacheteNonRootBranch.getUpstreamBranch().getName()}");
      return;
    }

    var localFullName = trackingInfo.getLocalBranch().getFullName();
    var parentLocalFullName = parentTrackingInfo.getLocalBranch().getFullName();
    var refspecChildParent = "${localFullName}:${parentLocalFullName}";

    // Remote set to '.' (dot) is just the local repository.
    new FetchBackgroundable(project, gitRepository, refspecChildParent, GitRemote.DOT,
        GitMacheteBundle.message("action.fast-forward.task.title")).queue();
  }
}
