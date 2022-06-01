package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.RESET;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import io.vavr.collection.List;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.ResetBranchToRemoteInfoDialog;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public abstract class BaseResetBranchToRemoteAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToRemoteStatusDependentAction {

  public static final String RESET_INFO_SHOWN = "git-machete.reset.info.shown";

  private static final String VCS_NOTIFIER_TITLE = getString(
      "action.GitMachete.BaseResetBranchToRemoteAction.notification.title");

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BaseResetBranchToRemoteAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getActionNameForDescription() {
    return getString("action.GitMachete.BaseResetBranchToRemoteAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.BaseResetBranchToRemoteAction.description.enabled");
  }

  @Override
  public List<SyncToRemoteStatus> getEligibleStatuses() {
    return List.of(
        SyncToRemoteStatus.AheadOfRemote,
        SyncToRemoteStatus.BehindRemote,
        SyncToRemoteStatus.DivergedFromAndNewerThanRemote,
        SyncToRemoteStatus.DivergedFromAndOlderThanRemote);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToRemoteStatusDependentActionUpdate(anActionEvent);

    val branch = getNameOfBranchUnderAction(anActionEvent);
    if (branch.isDefined()) {
      val isResettingCurrent = getCurrentBranchNameIfManaged(anActionEvent)
          .map(bn -> bn.equals(branch.get())).getOrElse(false);
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU) && isResettingCurrent) {
        anActionEvent.getPresentation().setText(() -> getActionName());
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    Project project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    val branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    val macheteRepository = getGitMacheteRepositorySnapshot(anActionEvent).getOrNull();

    if (gitRepository == null) {
      VcsNotifier.getInstance(project).notifyWarning(/* displayId */ null, VCS_NOTIFIER_TITLE,
          "Skipping the action because no Git repository is selected");
      return;
    }

    if (branchName == null) {
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null, VCS_NOTIFIER_TITLE,
          "Internal error occurred. For more information see IDE log file");
      return;
    }

    if (macheteRepository == null) {
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null, VCS_NOTIFIER_TITLE,
          "Internal error occurred. For more information see IDE log file");
      return;
    }

    val localBranch = getManagedBranchByName(anActionEvent, branchName).getOrNull();
    if (localBranch == null) {
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null, VCS_NOTIFIER_TITLE,
          "Cannot get local branch '${branchName}'");
      return;
    }

    val remoteTrackingBranch = localBranch.getRemoteTrackingBranch().getOrNull();
    if (remoteTrackingBranch == null) {
      String message = "Branch '${localBranch.getName()}' doesn't have remote tracking branch, so cannot be reset";
      log().warn(message);
      VcsNotifier.getInstance(project).notifyWarning(/* displayId */ null, VCS_NOTIFIER_TITLE, message);
      return;
    }

    // if key is missing the default value (false) is returned
    if (!PropertiesComponent.getInstance().getBoolean(RESET_INFO_SHOWN)) {

      String currentCommitSha = localBranch.getPointedCommit().getHash();
      if (currentCommitSha.length() == 40) {
        currentCommitSha = currentCommitSha.substring(0, 15);
      }

      final int okCancelDialogResult = MessageUtil.showOkCancelDialog(
          getString("action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.title"),
          getString("action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.message").format(
              branchName,
              remoteTrackingBranch.getName(),
              currentCommitSha),
          getString("action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.ok-text"),
          Messages.getCancelButton(),
          Messages.getInformationIcon(),
          new ResetBranchToRemoteInfoDialog(),
          project);
      if (okCancelDialogResult != Messages.OK) {
        return;
      }
    }

    // Required to avoid reset with uncommitted changes and file cache conflicts
    FileDocumentManager.getInstance().saveAllDocuments();

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
    if (branchName.equals(currentBranchName)) {
      doResetCurrentBranchToRemoteWithKeep(project, gitRepository, localBranch, remoteTrackingBranch);
    } else {
      doResetNonCurrentBranchToRemoteWithKeep(project, gitRepository, localBranch, remoteTrackingBranch);
    }
  }

  private void doResetNonCurrentBranchToRemoteWithKeep(Project project,
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IRemoteTrackingBranchReference remoteTrackingBranch) {
    val refspecFromRemoteToLocal = createRefspec(
        remoteTrackingBranch.getFullName(), localBranch.getFullName(), /* allowNonFastForward */ true);

    new FetchBackgroundable(
        project,
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromRemoteToLocal,
        getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"),
        getString("action.GitMachete.BaseResetBranchToRemoteAction.task-subtitle"),
        getString("action.GitMachete.BaseResetBranchToRemoteAction.notification.title.reset-fail")
            .format(localBranch.getName()),
        getString("action.GitMachete.BaseResetBranchToRemoteAction.notification.title.reset-success")
            .format(localBranch.getName()))
                .queue();
  }

  protected void doResetCurrentBranchToRemoteWithKeep(
      Project project,
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IRemoteTrackingBranchReference remoteTrackingBranch) {

    new Task.Backgroundable(project,
        getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"),
        /* canBeCancelled */ true) {

      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        val localBranchName = localBranch.getName();
        val remoteTrackingBranchName = remoteTrackingBranch.getName();
        log().debug(() -> "Resetting '${localBranchName}' to '${remoteTrackingBranchName}'");

        try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(project,
            getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"))) {
          GitLineHandler resetHandler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.RESET);
          resetHandler.addParameters("--keep");
          resetHandler.addParameters(remoteTrackingBranchName);
          resetHandler.endOptions();

          val localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(gitRepository.getRoot(), RESET);
          resetHandler.addLineListener(localChangesDetector);

          GitCommandResult result = Git.getInstance().runCommand(resetHandler);

          if (result.success()) {
            VcsNotifier.getInstance(project).notifySuccess( /* displayId */ null,
                /* title */ "",
                getString("action.GitMachete.BaseResetBranchToRemoteAction.notification.title.reset-success")
                    .format(localBranchName));
            log().debug(() -> "Branch '${localBranchName}' has been reset to '${remoteTrackingBranchName}");

          } else if (localChangesDetector.wasMessageDetected()) {
            LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project,
                /* displayId */ null,
                gitRepository.getRoot(),
                /* operationName */ "Reset",
                localChangesDetector.getRelativeFilePaths());

          } else {
            log().error(result.getErrorOutputAsJoinedString());
            VcsNotifier.getInstance(project).notifyError(/* displayId */ null, VCS_NOTIFIER_TITLE,
                result.getErrorOutputAsHtmlString());
          }

          val repositoryRoot = gitRepository.getRootDirectory();
          GitRepositoryManager.getInstance(project).updateRepository(repositoryRoot);
          VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false, repositoryRoot);
        }
      }
    }.queue();
  }
}
