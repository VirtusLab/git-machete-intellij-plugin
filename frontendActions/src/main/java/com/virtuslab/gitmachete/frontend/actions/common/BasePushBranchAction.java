package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
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
public abstract class BasePushBranchAction extends DumbAwareAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  protected final List<SyncToRemoteStatus.Relation> PUSH_ENABLING_STATUSES = List.of(
      SyncToRemoteStatus.Relation.Ahead,
      SyncToRemoteStatus.Relation.DivergedAndNewerThanRemote,
      SyncToRemoteStatus.Relation.DivergedAndOlderThanRemote,
      SyncToRemoteStatus.Relation.Untracked);

  /**
   * Bear in mind that {@link AnAction#beforeActionPerformedUpdate} is called before each action.
   * (For more details check {@link com.intellij.openapi.actionSystem.ex.ActionUtil} as well.)
   * The {@link AnActionEvent} argument passed to before-called {@link AnAction#update} is the same one that is passed here.
   * This gives us certainty that all checks from actions' update implementations will be performed
   * and all data available via data datakeys in those {@code update} implementations will still do be available
   * in {@link BasePushBranchAction#actionPerformed} implementations.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);

  @UIEffect
  public void doPush(Project project, java.util.List<GitRepository> preselectedRepositories, String branchName) {
    if (preselectedRepositories.size() > 0) {
      @Nullable
      GitLocalBranch localBranch = preselectedRepositories.get(0).getBranches().findLocalBranch(branchName);

      if (localBranch != null) {
        new VcsPushDialog(project,
            /* allRepositories */ preselectedRepositories,
            preselectedRepositories,
            /* currentRepo */ null,
            GitPushSource.create(localBranch)).show();
      } else {
        LOG.warn("Skipping the action because no provided branch ${branchName} was not found in repository");
      }

    } else {
      LOG.warn("Skipping the action because no VCS repository is selected");
    }
  }
}
