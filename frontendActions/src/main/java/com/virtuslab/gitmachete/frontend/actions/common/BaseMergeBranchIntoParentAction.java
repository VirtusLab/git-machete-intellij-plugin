package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentBranchNameIfManaged;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getFetchBackgroundable;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteBranchByName;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actionids.ActionPlaces;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BaseMergeBranchIntoParentAction extends GitMacheteRepositoryReadyAction implements IBranchNameProvider {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

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
      presentation.setDescription("Merge disabled due to undefined branch name");
      return;
    }

    var gitMacheteBranch = getGitMacheteBranchByName(anActionEvent, branchName.get());
    if (gitMacheteBranch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Merge disabled due to undefined machete branch");
      return;
    }

    if (gitMacheteBranch.get().isRootBranch()) {
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription("Root branch '${branchName}' cannot be merged");
      } else { //contextmenu
        // in case of root branch we do not want to show this option at all
        presentation.setEnabledAndVisible(false);
      }
      return;
    }

    var gitMacheteNonRoot = gitMacheteBranch.get().asNonRootBranch();
    var syncToParentStatus = gitMacheteNonRoot.getSyncToParentStatus();

    if (SyncToParentStatus.InSync == syncToParentStatus) {

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText("Merge Current Branch Into Parent");
      }

      var parentName = gitMacheteNonRoot.getUpstreamBranch().getName();
      presentation.setDescription("Merge branch '${branchName.get()}' into '${parentName}'");

    } else {
      presentation.setEnabled(false);
      var desc = Match(syncToParentStatus).of(
          Case($(SyncToParentStatus.InSync), "in sync to its parent"),
          Case($(SyncToParentStatus.InSyncButForkPointOff), "in sync to its parent but fork point is off"),
          Case($(SyncToParentStatus.MergedToParent), "merge into parent"),
          Case($(SyncToParentStatus.OutOfSync), "out of sync to its parent"),
          Case($(), "in unknown status '${syncToParentStatus.toString()}' to its parent"));

      presentation.setDescription("Merge disabled because the branch is ${desc}");
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
        assert gitMacheteBranch.get().isNonRootBranch() : "Provided machete branch to merge is a root";
        doMerge(project, gitRepository.get(), gitMacheteBranch.get().asNonRootBranch());
      } else {
        LOG.warn("Skipping the action because no VCS repository is selected");
      }
    } else {
      LOG.warn("Skipping the action because machete branch to merge is undefined");
    }
  }

  private void doMerge(Project project, GitRepository gitRepository, BaseGitMacheteNonRootBranch gitMacheteNonRootBranch) {
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

    // On the other hand this refspec has no '+' sign.
    // This is because the fetch from local remotes to local heads must behave fast-forward-like.
    var refspecRemoteLocal = "${localFullName}:${parentLocalFullName}";

    // Remote set to '.' (dot) is just the local repository.
    getFetchBackgroundable(project, gitRepository, refspecRemoteLocal, GitRemote.DOT, "Merging...").queue();
  }
}
