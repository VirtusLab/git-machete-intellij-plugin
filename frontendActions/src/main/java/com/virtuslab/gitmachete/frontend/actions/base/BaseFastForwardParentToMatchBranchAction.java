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
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedVcsRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;

@CustomLog
public abstract class BaseFastForwardParentToMatchBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyProject,
      IExpectsKeySelectedVcsRepository {

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    if (branchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Fast forward disabled due to undefined branch name");
      return;
    }

    var gitMacheteBranch = getGitMacheteBranchByName(anActionEvent, branchName.get());
    if (gitMacheteBranch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Fast forward disabled due to undefined machete branch");
      return;
    }

    if (gitMacheteBranch.get().isRootBranch()) {
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription("Root branch '${branchName.get()}' cannot be fast-forwarded");
      } else { //contextmenu
        // in case of root branch we do not want to show this option at all
        presentation.setEnabledAndVisible(false);
      }
      return;
    }

    var gitMacheteNonRoot = gitMacheteBranch.get().asNonRootBranch();
    var syncToParentStatus = gitMacheteNonRoot.getSyncToParentStatus();

    if (syncToParentStatus == SyncToParentStatus.InSync) {

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText("Fast Forward Parent To Match Current Branch");
      }

      var parentName = gitMacheteNonRoot.getUpstreamBranch().getName();
      presentation.setDescription("Fast forward branch '${parentName}' to match '${branchName.get()}'");

    } else {
      presentation.setEnabled(false);
      var desc = Match(syncToParentStatus).of(
          Case($(SyncToParentStatus.InSyncButForkPointOff), "in sync to its parent but fork point is off"),
          Case($(SyncToParentStatus.MergedToParent), "merged into parent"),
          Case($(SyncToParentStatus.OutOfSync), "out of sync to its parent"),
          Case($(), "in unknown status '${syncToParentStatus.toString()}' to its parent"));

      presentation.setDescription("Fast forward disabled because the branch is ${desc}");
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedVcsRepository(anActionEvent);
    var gitMacheteBranch = getNameOfBranchUnderAction(anActionEvent).flatMap(b -> getGitMacheteBranchByName(anActionEvent, b));

    if (gitMacheteBranch.isDefined()) {
      if (gitRepository.isDefined()) {
        assert gitMacheteBranch.get().isNonRootBranch() : "Provided machete branch to fast forward is a root";
        doFastForward(project, gitRepository.get(), gitMacheteBranch.get().asNonRootBranch());
      } else {
        LOG.warn("Skipping the action because no VCS repository is selected");
      }
    } else {
      LOG.warn("Skipping the action because machete branch to fast forward is undefined");
    }
  }

  private void doFastForward(Project project,
      GitRepository gitRepository,
      IGitMacheteNonRootBranch gitMacheteNonRootBranch) {
    var trackingInfo = gitRepository.getBranchTrackInfo(gitMacheteNonRootBranch.getName());
    var parentTrackingInfo = gitRepository.getBranchTrackInfo(gitMacheteNonRootBranch.getUpstreamBranch().getName());

    if (trackingInfo == null) {
      LOG.warn("No branch tracking info for branch ${gitMacheteNonRootBranch.getName()}");
      return;
    } else if (parentTrackingInfo == null) {
      LOG.warn("No branch tracking info for parent branch ${gitMacheteNonRootBranch.getUpstreamBranch().getName()}");
      return;
    }

    var localFullName = trackingInfo.getLocalBranch().getFullName();
    var parentLocalFullName = parentTrackingInfo.getLocalBranch().getFullName();
    var refspecChildParent = "${localFullName}:${parentLocalFullName}";

    // Remote set to '.' (dot) is just the local repository.
    new FetchBackgroundable(project, gitRepository, refspecChildParent, GitRemote.DOT, "Fast Forwarding...").queue();
  }
}
