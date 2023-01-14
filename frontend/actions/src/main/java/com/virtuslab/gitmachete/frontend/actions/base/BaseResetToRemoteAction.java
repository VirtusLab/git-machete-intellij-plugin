package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
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
import com.virtuslab.gitmachete.frontend.actions.backgroundables.ResetCurrentToRemoteBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DoNotAskOption;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class, StringEscapeUtils.class})
public abstract class BaseResetToRemoteAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToRemoteStatusDependentAction {

  public static final String SHOW_RESET_INFO = "git-machete.reset.info.show";

  public static final String VCS_NOTIFIER_TITLE = getString(
      "action.GitMachete.BaseResetToRemoteAction.notification.title");

  @Override
  protected boolean isSideEffecting() {
    return true;
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
  @ContinuesInBackground
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

      val content = getString("action.GitMachete.BaseResetToRemoteAction.info-dialog.message.HTML").fmt(
          branchName.escapeHtml4(),
          remoteTrackingBranch.getName().escapeHtml4(),
          currentCommitSha);

      val resetInfoDialog = MessageDialogBuilder.okCancel(
          getString("action.GitMachete.BaseResetToRemoteAction.info-dialog.title"),
          content).icon(Messages.getInformationIcon()).doNotAsk(new DoNotAskOption(project, SHOW_RESET_INFO));

      if (!resetInfoDialog.ask(project)) {
        return;
      }
    }

    // It is required to avoid the reset with uncommitted changes and file cache conflicts.
    FileDocumentManager.getInstance().saveAllDocuments();

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    if (branchName.equals(currentBranchName)) {
      doResetCurrentBranchToRemoteWithKeep(gitRepository, localBranch, remoteTrackingBranch);
    } else {
      doResetNonCurrentBranchToRemoteWithKeep(gitRepository, localBranch, remoteTrackingBranch);
    }
  }

  @ContinuesInBackground
  private void doResetNonCurrentBranchToRemoteWithKeep(
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IRemoteTrackingBranchReference remoteTrackingBranch) {
    String refspecFromRemoteToLocal = createRefspec(
        remoteTrackingBranch.getFullName(), localBranch.getFullName(), /* allowNonFastForward */ true);

    new FetchBackgroundable(
        gitRepository,
        LOCAL_REPOSITORY_NAME,
        refspecFromRemoteToLocal,
        getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
        getNonHtmlString("action.GitMachete.BaseResetToRemoteAction.notification.title.reset-fail")
            .fmt(localBranch.getName()),
        getString("action.GitMachete.BaseResetToRemoteAction.notification.title.reset-success.HTML").fmt(localBranch.getName()))
            .queue();
  }

  @ContinuesInBackground
  protected void doResetCurrentBranchToRemoteWithKeep(
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IRemoteTrackingBranchReference remoteTrackingBranch) {

    val localBranchName = localBranch.getName();
    val remoteTrackingBranchName = remoteTrackingBranch.getName();

    new ResetCurrentToRemoteBackgroundable(
        getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
        localBranchName, remoteTrackingBranchName, gitRepository).queue();
  }
}
