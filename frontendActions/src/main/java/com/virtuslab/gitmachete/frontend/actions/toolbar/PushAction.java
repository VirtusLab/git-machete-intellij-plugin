package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 * </ul>
 */
public class PushAction extends AnAction implements DumbAware {

  private final List<SyncToRemoteStatus.Relation> PUSH_ENABLING_STATUSES = List.of(
      SyncToRemoteStatus.Relation.Ahead,
      SyncToRemoteStatus.Relation.Diverged,
      SyncToRemoteStatus.Relation.Untracked);

  @Override
  @UIEffect
  public void update(AnActionEvent e) {
    super.update(e);

    Option<String> currentBranchName = ActionUtils.getSelectedVcsRepository(e)
        .flatMap(ideaRepo -> Option.of(ideaRepo.getCurrentBranch()))
        .flatMap(ideaBranch -> Option.of(ideaBranch.getName()));

    if (currentBranchName.isDefined()) {
      Option<SyncToRemoteStatus> syncToRemoteStatus = ActionUtils.getGitMacheteRepository(e)
          .flatMap(repo -> repo.getBranchByName(currentBranchName.get()))
          .flatMap(branch -> Option.of(branch.getSyncToRemoteStatus()));

      boolean isEnabledAndVisible = syncToRemoteStatus.isDefined()
          && PUSH_ENABLING_STATUSES.contains(syncToRemoteStatus.get().getRelation());

      e.getPresentation().setEnabledAndVisible(isEnabledAndVisible);
    } else {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    Project project = ActionUtils.getProject(anActionEvent);

    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(anActionEvent);
    if (selectedVcsRepository.isDefined()) {
      new VcsPushDialog(project, selectedVcsRepository.toJavaList(), /* currentRepo */ null).show();
    }
  }
}
