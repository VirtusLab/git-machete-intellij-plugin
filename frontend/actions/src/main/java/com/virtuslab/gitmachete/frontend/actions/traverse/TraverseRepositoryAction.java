package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DivergedFromParentDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DivergedFromRemoteDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.PullApprovalDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.PushApprovalDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.TraverseApprovalDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.actions.navigation.CheckoutNextAction;
import com.virtuslab.gitmachete.frontend.actions.navigation.CheckoutPreviousAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.OverrideForkPointOfCurrentAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.PullCurrentAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.PushCurrentAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.RefreshStatusAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.ResetCurrentToRemoteAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.SlideOutCurrentAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.SyncCurrentToParentByRebaseAction;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedNonRoot;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public class TraverseRepositoryAction extends BaseGitMacheteRepositoryReadyAction implements IExpectsKeySelectedBranchName {

  @Untainted
  @I18nFormat({I18nConversionCategory.GENERAL})
  final String actionIdFormatString = getNonHtmlString("action.GitMachete.TraverseRepositoryAction.invoked-action.id");

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  public static final String SHOW_PULL_APPROVAL = "git-machete.pull.approval.show";
  public static final String SHOW_PUSH_APPROVAL = "git-machete.push.approval.show";
  public static final String SHOW_TRAVERSE_APPROVAL = "git-machete.traverse.approval.show";

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }
    presentation.setDescription(getNonHtmlString("action.GitMachete.TraverseRepositoryAction.description.text"));

    val repositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;

    if (branchLayout == null || branchLayout.getRootEntries().isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.TraverseRepositoryAction.description.empty-layout"));
      return;
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val repository = getSelectedGitRepository(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);

    if (branchLayout != null && branchLayout.getRootEntries().nonEmpty() && repository != null) {
      boolean traverseApproved = true;
      if (PropertiesComponent.getInstance().getBoolean(SHOW_TRAVERSE_APPROVAL, /* defaultValue */ true)) {

        val traverseApprovalDialogBuilder = MessageDialogBuilder.okCancel(
            getString("action.GitMachete.TraverseRepositoryAction.dialog.traverse-approval.title"),
            getString("action.GitMachete.TraverseRepositoryAction.dialog.traverse-approval.text.HTML"));

        traverseApprovalDialogBuilder
            .yesText(getString("action.GitMachete.TraverseRepositoryAction.dialog.traverse-approval.ok-text"))
            .icon(Messages.getQuestionIcon()).doNotAsk(new TraverseApprovalDialog());

        traverseApproved = traverseApprovalDialogBuilder.ask(getProject(anActionEvent));
      }

      if (traverseApproved) {
        var currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
        var previousEntry = currentBranchName != null ? branchLayout.findPreviousEntry(currentBranchName) : null;
        while (previousEntry != null) {
          val checkoutPreviousAction = ActionManager.getInstance()
              .getAction(actionIdFormatString.format(CheckoutPreviousAction.class.getSimpleName()));
          checkoutPreviousAction.actionPerformed(anActionEvent);
          currentBranchName = previousEntry.getName();
          previousEntry = branchLayout.findPreviousEntry(currentBranchName);
        }

        if (currentBranchName != null) {
          traverse(currentBranchName, anActionEvent);
        }

      }
    }
  }

  @UIEffect
  private void traverse(String branchName, AnActionEvent anActionEvent) {
    val gitMacheteBranch = getManagedBranchByName(anActionEvent, branchName);

    if (gitMacheteBranch != null) {
      val remoteTrackingBranch = gitMacheteBranch.getRemoteTrackingBranch();
      if (remoteTrackingBranch != null) {
        syncBranchToRemote(remoteTrackingBranch, gitMacheteBranch, anActionEvent);
      }
      if (gitMacheteBranch.isNonRoot()) {
        syncBranchToParent(gitMacheteBranch, anActionEvent);
        val processedGitMacheteBranch = getManagedBranchByName(anActionEvent, branchName);
        if (processedGitMacheteBranch != null && remoteTrackingBranch != null) {
          syncBranchToRemote(remoteTrackingBranch, processedGitMacheteBranch, anActionEvent);
        }
      }

      val branchLayout = getBranchLayout(anActionEvent);
      var nextBranch = branchLayout != null ? branchLayout.findNextEntry(branchName) : null;
      if (nextBranch != null) {
        val checkoutNextAction = ActionManager.getInstance()
            .getAction(actionIdFormatString.format(CheckoutNextAction.class.getSimpleName()));
        checkoutNextAction.actionPerformed(anActionEvent);
        traverse(nextBranch.getName(), anActionEvent);
      }
    }

  }

  @UIEffect
  private void syncBranchToRemote(IRemoteTrackingBranchReference remoteTrackingBranch, IManagedBranchSnapshot gitMacheteBranch,
      AnActionEvent anActionEvent) {
    SyncToRemoteStatus status = gitMacheteBranch.getRelationToRemote().getSyncToRemoteStatus();
    switch (status) {
      case AheadOfRemote :
        boolean pushApproved = true;
        if (PropertiesComponent.getInstance().getBoolean(SHOW_PUSH_APPROVAL, /* defaultValue */ true)) {
          val pushApprovalDialogBuilder = MessageDialogBuilder.okCancel(
              getString("action.GitMachete.TraverseRepositoryAction.dialog.push-verification.title"),
              getString("action.GitMachete.TraverseRepositoryAction.dialog.push-verification.text.HTML")
                  .format(gitMacheteBranch.getName(), remoteTrackingBranch.getRemoteName(), remoteTrackingBranch.getName()));

          pushApprovalDialogBuilder
              .yesText(getString("action.GitMachete.TraverseRepositoryAction.dialog.push-verification.ok-text"))
              .icon(Messages.getQuestionIcon()).doNotAsk(new PushApprovalDialog());

          pushApproved = pushApprovalDialogBuilder.ask(getProject(anActionEvent));
        }

        if (pushApproved) {
          val fastForwardPushAction = ActionManager.getInstance()
              .getAction(actionIdFormatString.format(PushCurrentAction.class.getSimpleName()));
          fastForwardPushAction.actionPerformed(anActionEvent);
        }
        break;
      case DivergedFromAndOlderThanRemote :
      case DivergedFromAndNewerThanRemote :
        val selectedAction = new DivergedFromRemoteDialog(getProject(anActionEvent), remoteTrackingBranch, gitMacheteBranch,
            status).showAndGetThePreferredAction();
        if (selectedAction == null) {
          log().debug(
              "Action selected for resolving divergence from remote is null: most likely the action has been canceled from Diverge-From-Remote-Dialog dialog");
          return;
        }
        switch (selectedAction) {
          case REBASE_ON_REMOTE :
            val rebaseCurrentToRemoteAction = ActionManager.getInstance()
                .getAction(actionIdFormatString.format(SyncCurrentToRemoteByRebaseAction.class.getSimpleName()));
            rebaseCurrentToRemoteAction.actionPerformed(anActionEvent);
            break;
          case MERGE_REMOTE_INTO_LOCAL :
            break;
          case RESET_ON_REMOTE :
            val resetCurrentToRemoteAction = ActionManager.getInstance()
                .getAction(actionIdFormatString.format(ResetCurrentToRemoteAction.class.getSimpleName()));
            resetCurrentToRemoteAction.actionPerformed(anActionEvent);
            break;
          case FORCE_PUSH :
            val pushAction = ActionManager.getInstance()
                .getAction(actionIdFormatString.format(PushCurrentAction.class.getSimpleName()));
            pushAction.actionPerformed(anActionEvent);
            break;
          default :
            break;
        }

        break;

      case BehindRemote :
        boolean pullApproved = true;
        if (PropertiesComponent.getInstance().getBoolean(SHOW_PULL_APPROVAL, /* defaultValue */ true)) {
          val pullApprovalDialogBuilder = MessageDialogBuilder.okCancel(
              getString("action.GitMachete.TraverseRepositoryAction.dialog.pull-verification.title"),
              getString("action.GitMachete.TraverseRepositoryAction.dialog.pull-verification.text.HTML")
                  .format(gitMacheteBranch.getName(), remoteTrackingBranch.getRemoteName(), remoteTrackingBranch.getName()));

          pullApprovalDialogBuilder
              .yesText(getString("action.GitMachete.TraverseRepositoryAction.dialog.pull-verification.ok-text"))
              .icon(Messages.getQuestionIcon()).doNotAsk(new PullApprovalDialog());

          pullApproved = pullApprovalDialogBuilder.ask(getProject(anActionEvent));
        }

        if (pullApproved) {
          val pullAction = ActionManager.getInstance()
              .getAction(actionIdFormatString.format(PullCurrentAction.class.getSimpleName()));
          pullAction.actionPerformed(anActionEvent);
        }
        break;
      default :
        break;
    }
    refreshBranchLayout(anActionEvent);
  }

  @UIEffect
  private void syncBranchToParent(@ConfirmedNonRoot IManagedBranchSnapshot gitMacheteBranch, AnActionEvent anActionEvent) {
    val gitMacheteNonRootBranch = gitMacheteBranch.asNonRoot();
    val syncToParentStatus = gitMacheteNonRootBranch.getSyncToParentStatus();
    switch (syncToParentStatus) {
      case MergedToParent :
        val slideOutCurrentAction = ActionManager.getInstance()
            .getAction(actionIdFormatString.format(SlideOutCurrentAction.class.getSimpleName()));
        slideOutCurrentAction.actionPerformed(anActionEvent);
        break;
      case InSyncButForkPointOff :
        val overrideForkPointAction = ActionManager.getInstance()
            .getAction(actionIdFormatString.format(OverrideForkPointOfCurrentAction.class.getSimpleName()));
        overrideForkPointAction.actionPerformed(anActionEvent);
        break;
      case OutOfSync :
        val nonRootBranch = gitMacheteBranch.asNonRoot();

        val selectedAction = new DivergedFromParentDialog(getProject(anActionEvent), nonRootBranch.getParent(), nonRootBranch)
            .showAndGetThePreferredAction();
        if (selectedAction == null) {
          log().debug(
              "Action selected for resolving divergence from parent is null: most likely the action has been canceled from Diverge-From-Remote-Dialog dialog");
          return;
        }
        switch (selectedAction) {
          case RESET_TO_PARENT :
            val resetCurrentToParentAction = ActionManager.getInstance()
                .getAction(actionIdFormatString.format(ResetCurrentToParentAction.class.getSimpleName()));
            resetCurrentToParentAction.actionPerformed(anActionEvent);
            break;
          case REBASE_ON_PARENT :
            val syncToParentByRebaseAction = ActionManager.getInstance()
                .getAction(actionIdFormatString.format(SyncCurrentToParentByRebaseAction.class.getSimpleName()));
            syncToParentByRebaseAction.actionPerformed(anActionEvent);
            break;
          case MERGE_PARENT_INTO_CURRENT :
            val syncToParentByMergeAction = ActionManager.getInstance()
                .getAction(actionIdFormatString.format(SyncCurrentToParentByMergeAction.class.getSimpleName()));
            syncToParentByMergeAction.actionPerformed(anActionEvent);
            break;
          default :
            break;
        }
        break;
      default :
        break;
    }
    refreshBranchLayout(anActionEvent);
  }

  @UIEffect
  private void refreshBranchLayout(AnActionEvent anActionEvent) {
    val refreshStatusAction = ActionManager.getInstance()
        .getAction(actionIdFormatString.format(RefreshStatusAction.class.getSimpleName()));
    refreshStatusAction.actionPerformed(anActionEvent);
  }
}
