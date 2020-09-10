package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
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
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.repo.GitRepository;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.ResetBranchToRemoteInfoDialog;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseResetBranchToRemoteAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToRemoteStatusDependentAction {

  public static final String RESET_INFO_SHOWN = "git-machete.reset.info.shown";

  private static final String VCS_NOTIFIER_TITLE = getString(
      "action.GitMachete.BaseResetBranchToRemoteAction.notification.title");

  @Override
  public IEnhancedLambdaLogger log() {
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
  public List<SyncToRemoteStatus.Relation> getEligibleRelations() {
    return List.of(
        SyncToRemoteStatus.Relation.AheadOfRemote,
        SyncToRemoteStatus.Relation.BehindRemote,
        SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote,
        SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToRemoteStatusDependentActionUpdate(anActionEvent);

    var branch = getNameOfBranchUnderAction(anActionEvent);
    if (branch.isDefined()) {
      var isResettingCurrent = getCurrentBranchNameIfManaged(anActionEvent)
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
    var gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    var macheteRepository = getGitMacheteRepositorySnapshot(anActionEvent).getOrNull();

    if (gitRepository == null) {
      VcsNotifier.getInstance(project).notifyWarning(VCS_NOTIFIER_TITLE,
          "Skipping the action because no Git repository is selected");
      return;
    }

    if (branchName == null) {
      VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE,
          "Internal error occurred. For more information see IDE log file");
      return;
    }

    if (macheteRepository == null) {
      VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE,
          "Internal error occurred. For more information see IDE log file");
      return;
    }

    var localBranch = getManagedBranchByName(anActionEvent, branchName).getOrNull();
    if (localBranch == null) {
      VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE, "Cannot get local branch '${branchName}'");
      return;
    }

    var remoteTrackingBranch = localBranch.getRemoteTrackingBranch().getOrNull();
    if (remoteTrackingBranch == null) {
      String message = "Branch '${localBranch.getName()}' doesn't have remote tracking branch, so cannot be reset";
      log().warn(message);
      VcsNotifier.getInstance(project).notifyWarning(VCS_NOTIFIER_TITLE, message);
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
          format(getString("action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.message"),
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

    var currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
    if (branchName.equals(currentBranchName)) {
      doResetCurrentBranchToRemoteWithKeep(project, gitRepository, localBranch, remoteTrackingBranch);
    } else {
      doResetNonCurrentBranchToRemoteWithKeep(project, gitRepository, localBranch, remoteTrackingBranch);
    }
  }

  private void doResetNonCurrentBranchToRemoteWithKeep(Project project,
      GitRepository gitRepository,
      IManagedBranchSnapshot localBranch,
      IRemoteTrackingBranchReference remoteTrackingBranch) {
    var refspecFromRemoteToLocal = createRefspec(
        remoteTrackingBranch.getFullName(), localBranch.getFullName(), /* allowNonFastForward */ true);

    new FetchBackgroundable(
        project,
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromRemoteToLocal,
        getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"),
        format(getString("action.GitMachete.BaseResetBranchToRemoteAction.notification.title.reset-fail"),
            localBranch.getName()),
        format(getString("action.GitMachete.BaseResetBranchToRemoteAction.notification.title.reset-success"),
            localBranch.getName()))
                .queue();
  }

  protected void doResetCurrentBranchToRemoteWithKeep(
      Project project,
      GitRepository gitRepository,
      IManagedBranchSnapshot localBranch,
      IRemoteTrackingBranchReference remoteTrackingBranch) {

    new Task.Backgroundable(project,
        getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"),
        /* canBeCancelled */ true) {

      @Override
      public void run(ProgressIndicator indicator) {
        var localBranchName = localBranch.getName();
        var remoteTrackingBranchName = remoteTrackingBranch.getName();
        log().debug(() -> "Resetting '${localBranchName}' to '${remoteTrackingBranchName}'");

        try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(project,
            getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"))) {
          GitLineHandler resetHandler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.RESET);
          resetHandler.addParameters("--keep");
          resetHandler.addParameters(remoteTrackingBranchName);
          resetHandler.endOptions();

          var localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(gitRepository.getRoot(), RESET);
          resetHandler.addLineListener(localChangesDetector);

          GitCommandResult result = Git.getInstance().runCommand(resetHandler);

          if (result.success()) {
            VcsNotifier.getInstance(project)
                .notifySuccess(
                    format(getString("action.GitMachete.BaseResetBranchToRemoteAction.notification.title.reset-success"),
                        localBranchName));
            log().debug(() -> "Branch '${localBranchName}' has been reset to '${remoteTrackingBranchName}");

          } else if (localChangesDetector.wasMessageDetected()) {
            LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project,
                gitRepository.getRoot(),
                "Reset",
                localChangesDetector.getRelativeFilePaths());

          } else {
            log().error(result.getErrorOutputAsJoinedString());
            VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE, result.getErrorOutputAsHtmlString());
          }
        }
      }
    }.queue();
  }
}
