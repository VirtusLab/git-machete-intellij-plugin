package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
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
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import git4idea.GitReference;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
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
import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.ResetBranchToRemoteInfoDialog;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class, StringEscapeUtils.class})
@CustomLog
public abstract class BaseResetToRemoteAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToRemoteStatusDependentAction {

  public static final String SHOW_RESET_INFO = "git-machete.reset.info.show";

  private static final String VCS_NOTIFIER_TITLE = getString(
      "action.GitMachete.BaseResetToRemoteAction.notification.title");

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BaseResetToRemoteAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getActionNameForDescription() {
    return getString("action.GitMachete.BaseResetToRemoteAction.description-action-name");
  }

  @Override
  public @Untainted @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getNonHtmlString("action.GitMachete.BaseResetToRemoteAction.description.enabled");
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
    if (branch != null) {
      val currentBranchIfManaged = getCurrentBranchNameIfManaged(anActionEvent);
      val isResettingCurrent = currentBranchIfManaged != null && currentBranchIfManaged.equals(branch);
      if (anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU) && isResettingCurrent) {
        anActionEvent.getPresentation().setText(getActionName());
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val macheteRepository = getGitMacheteRepositorySnapshot(anActionEvent);

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

    val localBranch = getManagedBranchByName(anActionEvent, branchName);
    if (localBranch == null) {
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null, VCS_NOTIFIER_TITLE,
          "Cannot get local branch '${branchName}'");
      return;
    }

    val remoteTrackingBranch = localBranch.getRemoteTrackingBranch();
    if (remoteTrackingBranch == null) {
      val message = "Branch '${localBranch.getName()}' doesn't have remote tracking branch, so cannot be reset";
      log().warn(message);
      VcsNotifier.getInstance(project).notifyWarning(/* displayId */ null, VCS_NOTIFIER_TITLE, message);
      return;
    }

    if (PropertiesComponent.getInstance(project).getBoolean(SHOW_RESET_INFO, /* defaultValue */ true)) {

      String currentCommitSha = localBranch.getPointedCommit().getHash();
      if (currentCommitSha.length() == 40) {
        currentCommitSha = currentCommitSha.substring(0, 15);
      }
      val dialogBuilder = MessageDialogBuilder.okCancel(
          getString("action.GitMachete.BaseResetToRemoteAction.info-dialog.title"),
          getString("action.GitMachete.BaseResetToRemoteAction.info-dialog.message.HTML").format(
              branchName.escapeHtml4(),
              remoteTrackingBranch.getName().escapeHtml4(),
              currentCommitSha));

      dialogBuilder
          .icon(Messages.getInformationIcon())
          .doNotAsk(new ResetBranchToRemoteInfoDialog(project));

      val okCancelDialogResult = dialogBuilder.ask(project);

      if (!okCancelDialogResult) {
        return;
      }
    }

    // It is required to avoid the reset with uncommitted changes and file cache conflicts.
    FileDocumentManager.getInstance().saveAllDocuments();

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
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
    String refspecFromRemoteToLocal = createRefspec(
        remoteTrackingBranch.getFullName(), localBranch.getFullName(), /* allowNonFastForward */ true);

    new FetchBackgroundable(
        project,
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromRemoteToLocal,
        getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
        getNonHtmlString("action.GitMachete.BaseResetToRemoteAction.task-subtitle"),
        getNonHtmlString("action.GitMachete.BaseResetToRemoteAction.notification.title.reset-fail")
            .format(localBranch.getName()),
        getString("action.GitMachete.BaseResetToRemoteAction.notification.title.reset-success.HTML")
            .format(localBranch.getName()))
                .queue();
  }

  protected void doResetCurrentBranchToRemoteWithKeep(
      Project project,
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IRemoteTrackingBranchReference remoteTrackingBranch) {

    new Task.Backgroundable(project,
        getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
        /* canBeCancelled */ true) {

      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        val localBranchName = localBranch.getName();
        val remoteTrackingBranchName = remoteTrackingBranch.getName();
        log().debug(() -> "Resetting '${localBranchName}' to '${remoteTrackingBranchName}'");

        try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(project,
            getString("action.GitMachete.BaseResetToRemoteAction.task-title"))) {
          val resetHandler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.RESET);
          resetHandler.addParameters("--keep");
          resetHandler.addParameters(remoteTrackingBranchName);
          resetHandler.endOptions();

          val localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(
              gitRepository.getRoot(), RESET);
          resetHandler.addLineListener(localChangesDetector);

          val result = Git.getInstance().runCommand(resetHandler);

          if (result.success()) {
            VcsNotifier.getInstance(project).notifySuccess( /* displayId */ null,
                /* title */ "",
                getString("action.GitMachete.BaseResetToRemoteAction.notification.title.reset-success.HTML")
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
