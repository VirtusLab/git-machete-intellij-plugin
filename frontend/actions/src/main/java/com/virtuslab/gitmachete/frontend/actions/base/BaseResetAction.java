package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.RESET;

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
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.frontend.actions.dialogs.ResetBranchToRemoteInfoDialog;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public abstract class BaseResetAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider {

  public static final String SHOW_RESET_INFO = "git-machete.reset.info.show";

  private static final String VCS_NOTIFIER_TITLE = getString(
      "action.GitMachete.BaseResetToRemoteAction.notification.title");

  @Override
  public LambdaLogger log() {
    return LOG;
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

    val targetBranch = getTargetBranch(anActionEvent);
    if (targetBranch == null) {
      val message = "The target branch for resetting '${localBranch.getName()}' to does not exist, so cannot be reset";
      log().warn(message);
      VcsNotifier.getInstance(project).notifyWarning(/* displayId */ null, VCS_NOTIFIER_TITLE, message);
      return;
    }

    if (PropertiesComponent.getInstance().getBoolean(SHOW_RESET_INFO, /* defaultValue */ true)) {

      String currentCommitSha = localBranch.getPointedCommit().getHash();
      if (currentCommitSha.length() == 40) {
        currentCommitSha = currentCommitSha.substring(0, 15);
      }
      val dialogBuilder = MessageDialogBuilder.okCancel(
          getString("action.GitMachete.BaseResetToRemoteAction.info-dialog.title"),
          getString("action.GitMachete.BaseResetAction.info-dialog.message.HTML").format(
              branchName,
              getRelationToTargetBranch(),
              getTargetBranchName(targetBranch),
              getResetOptionsString(),
              currentCommitSha));

      dialogBuilder.yesText(getString("action.GitMachete.BaseResetToRemoteAction.info-dialog.ok-text"))
          .noText(Messages.getCancelButton())
          .icon(Messages.getInformationIcon())
          .doNotAsk(new ResetBranchToRemoteInfoDialog());

      val okCancelDialogResult = dialogBuilder.ask(project);

      if (!okCancelDialogResult) {
        return;
      }
    }

    // Required to avoid reset with uncommitted changes and file cache conflicts
    FileDocumentManager.getInstance().saveAllDocuments();

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    if (branchName.equals(currentBranchName)) {
      doResetCurrentToTarget(project, gitRepository, localBranch, targetBranch);
    } else {
      handleResetNonCurrentToTarget(project, gitRepository, localBranch, targetBranch);
    }
  }

  protected abstract String getResetOptionsString();
  protected abstract String getRelationToTargetBranch();

  protected abstract String getTargetBranchName(IBranchReference targetBranch);
  protected abstract @Nullable IBranchReference getTargetBranch(AnActionEvent anActionEvent);

  protected abstract void handleResetNonCurrentToTarget(Project project,
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IBranchReference targetBranchReference);

  protected void doResetCurrentToTarget(
      Project project,
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IBranchReference targetBranch) {

    new Task.Backgroundable(project,
        getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
        /* canBeCancelled */ true) {

      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        val localBranchName = localBranch.getName();
        val targetBranchName = getTargetBranchName(targetBranch);
        log().debug(() -> "Resetting '${localBranchName}' to '${targetBranchName}'");

        try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(project,
            getString("action.GitMachete.BaseResetToRemoteAction.task-title"))) {
          val resetHandler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.RESET);
          val resetOptionsString = getResetOptionsString();
          if (!resetOptionsString.isEmpty()) {
            resetHandler.addParameters(resetOptionsString);
          }
          resetHandler.addParameters(targetBranchName);
          resetHandler.endOptions();

          val localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(
              gitRepository.getRoot(), RESET);
          resetHandler.addLineListener(localChangesDetector);

          val result = Git.getInstance().runCommand(resetHandler);

          if (result.success()) {
            VcsNotifier.getInstance(project).notifySuccess( /* displayId */ null,
                /* title */ "",
                getString("action.GitMachete.BaseResetAction.notification.title.reset-success.HTML")
                    .format(localBranchName, getRelationToTargetBranch()));
            log().debug(() -> "Branch '${localBranchName}' has been reset to '${targetBranchName}");

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
