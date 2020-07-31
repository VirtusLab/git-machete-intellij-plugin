package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.PullBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BasePullBranchFastForwardOnlyAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject,
      ISyncToRemoteStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getDescriptionActionName() {
    return getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.description-action-name");
  }

  @Override
  public List<SyncToRemoteStatus.Relation> getEligibleRelations() {
    return List.of(
        SyncToRemoteStatus.Relation.BehindRemote,
        SyncToRemoteStatus.Relation.InSyncToRemote);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    syncToRemoteStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var localBranchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    var gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent).getOrNull();

    if (localBranchName != null && gitRepository != null && gitMacheteRepositorySnapshot != null) {
      var localBranch = gitMacheteRepositorySnapshot.getManagedBranchByName(localBranchName).getOrNull();
      if (localBranch == null) {
        log().warn("Branch '${localBranchName}' not found or not managed by Git Machete");
        return;
      }
      var remoteBranch = localBranch.getRemoteTrackingBranch().getOrNull();
      if (remoteBranch == null) {
        // T_DO: add warning!
        log().warn("Branch '${localBranchName}' does not have a remote tracking branch");
        return;
      }

      var currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
      if (localBranchName.equals(currentBranchName)) {
        doPullCurrentBranchFastForwardOnly(project, gitRepository, localBranch, remoteBranch);
      } else {
        doPullNonCurrentBranchFastForwardOnly(project, gitRepository, localBranch, remoteBranch);
      }
    }
  }

  private void doPullCurrentBranchFastForwardOnly(Project project,
      GitRepository gitRepository,
      IGitMacheteBranch localBranch,
      IGitMacheteRemoteBranch remoteBranch) {

    var handler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.PULL);
    var remote = GitUtil.findRemoteByName(gitRepository, remoteBranch.getRemoteName());
    // T_DO: error!!!
    // T_DO: error!!!
    // T_DO: error!!!
    assert remote != null : "remote is null";
    handler.setUrls(remote.getUrls());
    handler.addParameters("--ff-only");
    handler.addParameters(remote.getName());
    var localBranchFullName = localBranch.getFullName();
    var remoteBranchFullName = remoteBranch.getFullName();
    // T_DO: localBranchFullName should be fixed!!!!!!!!!!!!!!! remoteBranch.getFullNameAsLocalBranch()
    handler.addParameters("+${localBranchFullName}:${remoteBranchFullName}");

    // T_DO: consistent args b/w PullBackgroundable and FetchBackgroundable
    new PullBackgroundable(project, gitRepository, handler, remoteBranch.getName()).queue();
  }

  private void doPullNonCurrentBranchFastForwardOnly(Project project,
      GitRepository gitRepository,
      IGitMacheteBranch localBranch,
      IGitMacheteRemoteBranch remoteBranch) {

    var localBranchFullName = localBranch.getFullName();
    var remoteBranchFullName = remoteBranch.getFullName();

    // Note the '+' sign preceding the refspec. It permits non-fast-forward updates.
    // This strategy is used to fetch branch from remote repository to local remotes.
    var refspecLocalRemote = "+${localBranchFullName}:${remoteBranchFullName}";

    // On the other hand this refspec has no '+' sign.
    // This is because the fetch from local remotes to local heads must behave fast-forward-like.
    var refspecRemoteLocal = "${remoteBranchFullName}:${localBranchFullName}";

    new FetchBackgroundable(project, gitRepository, remoteBranch.getRemoteName(), refspecLocalRemote,
        /* taskTitle */ getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.task-title")).queue();

    // T_DO: this should only be queued once the previous bgable finishes!
    // T_DO: this should only be queued once the previous bgable finishes!
    // T_DO: this should only be queued once the previous bgable finishes!
    // Remote set to '.' (dot) is just the local repository.
    new FetchBackgroundable(project, gitRepository, ".", refspecRemoteLocal,
        /* taskTitle */ getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.task-title")).queue();
  }
}
