package com.virtuslab.gitmachete.frontend.actions.base;

import static com.intellij.notification.NotificationType.INFORMATION;
import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.MERGE;
import static git4idea.update.GitUpdateSessionKt.getBodyForUpdateNotification;
import static git4idea.update.GitUpdateSessionKt.getTitleForUpdateNotification;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Map;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.history.Label;
import com.intellij.history.LocalHistory;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vcs.update.AbstractCommonUpdateAction;
import com.intellij.openapi.vcs.update.ActionInfo;
import com.intellij.openapi.vcs.update.UpdateInfoTree;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import com.intellij.vcs.ViewUpdateInfoNotification;
import git4idea.GitBranch;
import git4idea.GitRevisionNumber;
import git4idea.GitVcs;
import git4idea.branch.GitBranchPair;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.commands.GitUntrackedFilesOverwrittenByOperationDetector;
import git4idea.i18n.GitBundle;
import git4idea.merge.MergeChangeCollector;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.update.GitUpdateInfoAsLog;
import git4idea.update.GitUpdatedRanges;
import git4idea.util.GitUIUtil;
import git4idea.util.GitUntrackedFilesHelper;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BasePullBranchAction extends BaseGitMacheteRepositoryReadyAction
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
    return getString("action.GitMachete.BasePullBranchAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getDescriptionActionName() {
    return getString("action.GitMachete.BasePullBranchAction.description-action-name");
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
    var branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();

    if (branchName != null && gitRepository != null) {
      var name = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
      if (branchName.equals(name)) {
        doPull(project, gitRepository, branchName);
      } else {
        doFetch(project, gitRepository, branchName);
        getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      }
    }
  }

  private void doFetch(Project project, GitRepository gitRepository, String branchName) {
    var trackingInfo = gitRepository.getBranchTrackInfo(branchName);

    if (trackingInfo == null) {
      log().warn("No branch tracking info for branch ${branchName}");
      return;
    }

    var localFullName = trackingInfo.getLocalBranch().getFullName();
    var remoteFullName = trackingInfo.getRemoteBranch().getFullName();

    // Note the '+' sign preceding the refspec. It permits non-fast-forward updates.
    // This strategy is used to fetch branch from remote repository to local remotes.
    var refspecLocalRemote = "+${localFullName}:${remoteFullName}";

    // On the other hand this refspec has no '+' sign.
    // This is because the fetch from local remotes to local heads must behave fast-forward-like.
    var refspecRemoteLocal = "${remoteFullName}:${localFullName}";

    new FetchBackgroundable(project, gitRepository, refspecLocalRemote, trackingInfo.getRemote(),
        /* taskTitle */ getString("action.GitMachete.BasePullBranchAction.task-title"))
            .queue();

    // Remote set to '.' (dot) is just the local repository.
    new FetchBackgroundable(project, gitRepository, refspecRemoteLocal, GitRemote.DOT,
        /* taskTitle */ getString("action.GitMachete.BasePullBranchAction.task-title")).queue();
  }

  private void doPull(Project project, GitRepository gitRepository, String branchName) {
    var firstRemote = Option.ofOptional(gitRepository.getRemotes().stream().findFirst()).getOrNull();
    if (firstRemote == null) {
      LOG.error("Selected remote can't be null here.");
      return;
    }
    var remoteBranchName = "${firstRemote.getName()}/${branchName}";

    var handler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.PULL);
    handler.setUrls(firstRemote.getUrls());
    handler.addParameters("--ff-only");
    handler.addParameters(firstRemote.getName());
    handler.addParameters(branchName);

    new Task.Backgroundable(project, /* title */ "Pulling...", /* canBeCancelled */true) {
      @Override
      public void run(ProgressIndicator indicator) {
        var localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(gitRepository.getRoot(), MERGE);
        var untrackedFilesDetector = new GitUntrackedFilesOverwrittenByOperationDetector(gitRepository.getRoot());

        handler.addLineListener(localChangesDetector);
        handler.addLineListener(untrackedFilesDetector);

        Label beforeLabel = LocalHistory.getInstance().putSystemLabel(project, /* name */ "Before update");

        GitUpdatedRanges updatedRanges = deriveGitUpdatedRanges(project, gitRepository, remoteBranchName);

        String beforeRevision = gitRepository.getCurrentRevision();
        try (AccessToken ignore = DvcsUtil.workingTreeChangeStarted(project, /* activityName */ "Pull")) {
          GitCommandResult result = Git.getInstance().runCommand(() -> handler);

          if (beforeRevision != null) {
            GitRevisionNumber currentRev = new GitRevisionNumber(beforeRevision);
            handleResult(result,
                project,
                localChangesDetector,
                untrackedFilesDetector,
                gitRepository,
                currentRev,
                beforeLabel,
                updatedRanges);
          }
        }
      }
    }.queue();
  }

  @Nullable
  private static GitUpdatedRanges deriveGitUpdatedRanges(Project project, GitRepository gitRepository,
      String remoteBranchName) {
    GitUpdatedRanges updatedRanges = null;
    var currentBranch = gitRepository.getCurrentBranch();
    if (currentBranch != null) {
      String selectedBranch = StringUtil.trimStart(remoteBranchName, /* prefix */ "remotes/");
      GitBranch targetBranch = gitRepository.getBranches().findBranchByName(selectedBranch);
      if (targetBranch != null) {
        GitBranchPair refPair = new GitBranchPair(currentBranch, targetBranch);
        updatedRanges = GitUpdatedRanges.calcInitialPositions(project,
            java.util.Collections.singletonMap(gitRepository, refPair));
      } else {
        LOG.warn("Couldn't find the branch with name [" + selectedBranch + "]");
      }
    }
    return updatedRanges;
  }

  private static void handleResult(GitCommandResult result,
      Project project,
      GitLocalChangesWouldBeOverwrittenDetector localChangesDetector,
      GitUntrackedFilesOverwrittenByOperationDetector untrackedFilesDetector,
      GitRepository repository,
      GitRevisionNumber currentRev,
      Label beforeLabel,
      @Nullable GitUpdatedRanges updatedRanges) {
    VirtualFile root = repository.getRoot();

    final var OPERATION_NAME = "Pull";
    if (result.success()) {
      VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false, root);
      repository.update();
      if (updatedRanges != null &&
          AbstractCommonUpdateAction
              .showsCustomNotification(java.util.Collections.singletonList(GitVcs.getInstance(project)))) {
        var ranges = updatedRanges.calcCurrentPositions();
        GitUpdateInfoAsLog.NotificationData notificationData = new GitUpdateInfoAsLog(project, ranges)
            .calculateDataAndCreateLogTab();

        Notification notification;
        if (notificationData != null) {
          String title = getTitleForUpdateNotification(notificationData.getUpdatedFilesCount(),
              notificationData.getReceivedCommitsCount());
          String content = getBodyForUpdateNotification(notificationData.getFilteredCommitsCount());
          notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(title,
              content,
              INFORMATION,
              /* listener */ null);
          notification.addAction(NotificationAction.createSimple(GitBundle.messagePointer(
              "action.NotificationAction.GitMergeAction.text.view.commits"),
              notificationData.getViewCommitAction()));

        } else {
          notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
              VcsBundle.message("message.text.all.files.are.up.to.date"),
              /* content */ "", INFORMATION, /* listener */ null);
        }
        VcsNotifier.getInstance(project).notify(notification);

      } else {
        showUpdates(project, root, currentRev, beforeLabel);
      }

    } else if (localChangesDetector.wasMessageDetected()) {
      LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project,
          repository.getRoot(),
          OPERATION_NAME,
          localChangesDetector.getRelativeFilePaths());

    } else if (untrackedFilesDetector.wasMessageDetected()) {
      GitUntrackedFilesHelper.notifyUntrackedFilesOverwrittenBy(project,
          root,
          untrackedFilesDetector.getRelativeFilePaths(),
          OPERATION_NAME,
          /* description */ null);

    } else {
      GitUIUtil.notifyError(project,
          /* title */ "Git Pull Failed",
          result.getErrorOutputAsJoinedString(),
          /* important */ true,
          /* error */ null);
      repository.update();
    }
  }

  private static void showUpdates(Project project, VirtualFile root, GitRevisionNumber currentRev, Label beforeLabel) {
    final var OPERATION_NAME = "Pull";
    try {
      UpdatedFiles files = UpdatedFiles.create();
      MergeChangeCollector collector = new MergeChangeCollector(project, root, currentRev);
      collector.collect(files);

      GuiUtils.invokeLaterIfNeeded(() -> {
        ProjectLevelVcsManagerEx manager = (ProjectLevelVcsManagerEx) ProjectLevelVcsManager.getInstance(project);
        UpdateInfoTree tree = manager.showUpdateProjectInfo(files, OPERATION_NAME, ActionInfo.UPDATE, /* canceled */ false);
        if (tree != null) {
          tree.setBefore(beforeLabel);
          tree.setAfter(LocalHistory.getInstance().putSystemLabel(project, /* name */ "After update"));
          ViewUpdateInfoNotification.focusUpdateInfoTree(project, tree);
        }
      }, ModalityState.defaultModalityState());
    } catch (VcsException e) {
      GitVcs.getInstance(project).showErrors(java.util.Collections.singletonList(e), OPERATION_NAME);
    }
  }
}
