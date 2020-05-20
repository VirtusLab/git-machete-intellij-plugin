package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentBranchNameIfManaged;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteRepository;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.syncToRemoteStatusRelationToReadableBranchDescription;

import java.util.Collections;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
@CustomLog
public abstract class BasePushBranchAction extends GitMacheteRepositoryReadyAction implements IBranchNameProvider {

  protected final List<SyncToRemoteStatus.Relation> PUSH_ELIGIBLE_STATUSES = List.of(
      SyncToRemoteStatus.Relation.AheadOfRemote,
      SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote,
      SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote,
      SyncToRemoteStatus.Relation.Untracked);

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
      presentation.setDescription("Push disabled due to undefined branch name");
      return;
    }

    Option<SyncToRemoteStatus> syncToRemoteStatus = getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getBranchByName(branchName.get()))
        .map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Push disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    boolean isEnabled = PUSH_ELIGIBLE_STATUSES.contains(relation);

    if (isEnabled) {

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText("Push Current Branch");
      }

      presentation.setDescription("Push branch '${branchName.get()}' using push dialog");

    } else {
      presentation.setEnabled(false);
      String descriptionSpec = syncToRemoteStatusRelationToReadableBranchDescription(relation);
      presentation.setDescription("Push disabled because ${descriptionSpec}");
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedVcsRepository(anActionEvent);
    var branchName = getNameOfBranchUnderAction(anActionEvent);

    if (branchName.isDefined()) {
      if (gitRepository.isDefined()) {
        doPush(project, gitRepository.get(), branchName.get());
      } else {
        LOG.warn("Skipping the action because no VCS repository is selected");
      }
    } else {
      LOG.warn("Skipping the action because name of branch to push is undefined");
    }
  }

  @UIEffect
  private void doPush(Project project, GitRepository preselectedRepository, String branchName) {
    @Nullable GitLocalBranch localBranch = preselectedRepository.getBranches().findLocalBranch(branchName);

    if (localBranch != null) {
      java.util.List<GitRepository> selectedRepositories = Collections.singletonList(preselectedRepository);
      // Presented dialog shows commits for branches belonging to allRepositories, preselectedRepositories and currentRepo.
      // The second and the third one have higher priority of loading its commits.
      // From our perspective, we always have single (pre-selected) repository so we do not care about the priority.
      new VcsPushDialog(project,
          /* allRepositories */ selectedRepositories,
          /* preselectedRepositories */ selectedRepositories,
          /* currentRepo */ null,
          GitPushSource.create(localBranch)).show();
    } else {
      LOG.warn("Skipping the action because provided branch ${branchName} was not found in repository");
    }
  }
}
