package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus.FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.CheckoutBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.OverrideForkPointBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.RebaseOnParentBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.ResetCurrentToRemoteBrackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideOutBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.common.FastForwardMerge;
import com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DivergedFromParentDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DivergedFromRemoteDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.PullApprovalDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.PushApprovalDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.TraverseApprovalDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.actions.navigation.CheckoutNextAction;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedNonRoot;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public class TraverseAction extends BaseGitMacheteRepositoryReadyAction implements IExpectsKeySelectedBranchName {

  @Untainted
  @I18nFormat({I18nConversionCategory.GENERAL})
  final String actionIdFormatString = getNonHtmlString("action.GitMachete.TraverseAction.invoked-action.id");

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
    presentation.setDescription(getNonHtmlString("action.GitMachete.TraverseAction.description.text"));

    val repositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;

    if (branchLayout == null || branchLayout.getRootEntries().isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.TraverseAction.description.empty-layout"));
      return;
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);
    val project = getProject(anActionEvent);

    if (branchLayout != null && branchLayout.getRootEntries().nonEmpty() && gitRepository != null) {
      boolean traverseApproved = true;
      if (PropertiesComponent.getInstance(project).getBoolean(SHOW_TRAVERSE_APPROVAL, /* defaultValue */ true)) {

        val traverseApprovalDialogBuilder = MessageDialogBuilder.okCancel(
            getString("action.GitMachete.TraverseAction.dialog.traverse-approval.title"),
            getString("action.GitMachete.TraverseAction.dialog.traverse-approval.text.HTML"));

        traverseApprovalDialogBuilder
            .yesText(getString("action.GitMachete.TraverseAction.dialog.traverse-approval.ok-text"))
            .icon(Messages.getQuestionIcon()).doNotAsk(new TraverseApprovalDialog(project));

        traverseApproved = traverseApprovalDialogBuilder.ask(project);
      }

      if (traverseApproved) {
        var firstEntry = branchLayout.getRootEntries().find(entry -> true).getOrNull();
        if (firstEntry != null) {
          val firstEntryName = firstEntry.getName();
          checkoutAndTraverse(anActionEvent, firstEntryName, project, gitRepository);
        }

      }
    }
  }

  private void checkoutAndTraverse(AnActionEvent anActionEvent, String branchName, Project project,
      GitRepository gitRepository) {
    log().debug(() -> "Queuing '${branchName}' branch checkout background task");
    new CheckoutBackgroundable(project, getString("action.GitMachete.BaseCheckoutAction.task-title"), branchName,
        gitRepository) {
      @Override
      public void onSuccess() {
        super.onSuccess();
        traverse(branchName, anActionEvent, gitRepository);
      }
    }.queue();
  }

  @UIEffect
  private void traverse(String branchName, AnActionEvent anActionEvent, GitRepository gitRepository) {
    val gitMacheteBranch = getManagedBranchByName(anActionEvent, branchName);

    if (gitMacheteBranch != null) {
      if (gitMacheteBranch.isNonRoot()) {
        val remoteTrackingBranch = gitMacheteBranch.getRemoteTrackingBranch();
        val syncToRemoteStatus = gitMacheteBranch.getRelationToRemote().getSyncToRemoteStatus();
        boolean shouldSyncToParent;
        switch (syncToRemoteStatus) {
          case BehindRemote :
          case DivergedFromAndOlderThanRemote :
            shouldSyncToParent = false;
            break;
          default :
            shouldSyncToParent = true;
        }
        if (shouldSyncToParent) {
          syncBranchToParent(gitMacheteBranch, anActionEvent, gitRepository);
        }
      }

      val branchLayout = getBranchLayout(anActionEvent);
      var nextBranch = branchLayout != null ? branchLayout.findNextEntry(branchName) : null;
      if (nextBranch != null) {
        val checkoutNextAction = ActionManager.getInstance()
            .getAction(actionIdFormatString.format(CheckoutNextAction.class.getSimpleName()));
        checkoutNextAction.actionPerformed(anActionEvent);
        traverse(nextBranch.getName(), anActionEvent, gitRepository);
      }
    }

  }

  @UIEffect
  private void syncBranchToRemote(@Nullable IRemoteTrackingBranchReference remoteTrackingBranch, String branchName,
      AnActionEvent anActionEvent, GitRepository gitRepository) {
    val gitMacheteBranch = getManagedBranchByName(anActionEvent, branchName);
    if (gitMacheteBranch == null || remoteTrackingBranch == null) {
      return;
    }
    SyncToRemoteStatus status = gitMacheteBranch.getRelationToRemote().getSyncToRemoteStatus();
    val project = getProject(anActionEvent);
    val localBranchName = gitMacheteBranch.getName();
    @Nullable GitLocalBranch localBranch = gitRepository.getBranches().findLocalBranch(localBranchName);
    if (localBranch == null) {
      return;
    }
    val remoteTrackingBranchName = remoteTrackingBranch.getName();
    val graphTable = getGraphTable(anActionEvent);
    switch (status) {
      case AheadOfRemote :
        boolean pushApproved = true;
        if (PropertiesComponent.getInstance(project).getBoolean(SHOW_PUSH_APPROVAL, /* defaultValue */ true)) {
          val pushApprovalDialogBuilder = MessageDialogBuilder.okCancel(
              getString("action.GitMachete.TraverseAction.dialog.push-verification.title"),
              getString("action.GitMachete.TraverseAction.dialog.push-verification.text.HTML")
                  .format(localBranchName, remoteTrackingBranch.getRemoteName(), remoteTrackingBranchName));

          pushApprovalDialogBuilder
              .yesText(getString("action.GitMachete.TraverseAction.dialog.push-verification.ok-text"))
              .icon(Messages.getQuestionIcon()).doNotAsk(new PushApprovalDialog(project));

          pushApproved = pushApprovalDialogBuilder.ask(project);
        }
        if (pushApproved) {
          new GitPushDialog(project, List.of(gitRepository), GitPushSource.create(localBranch), /* isForcePushRequired */ false)
              .show();
        }
        break;
      case DivergedFromAndOlderThanRemote :
      case DivergedFromAndNewerThanRemote :
        val selectedAction = new DivergedFromRemoteDialog(project, remoteTrackingBranch, gitMacheteBranch,
            status).showAndGetThePreferredAction();
        if (selectedAction == null) {
          log().debug(
              "Action selected for resolving divergence from remote is null: most likely the action has been canceled from Diverge-From-Remote-Dialog dialog");
          return;
        }
        switch (selectedAction) {
          case RESET_ON_REMOTE :
            new ResetCurrentToRemoteBrackgroundable(project,
                getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
                /* canBeCancelled */ true, localBranchName, remoteTrackingBranchName, gitRepository);
            break;
          case FORCE_PUSH :
            new GitPushDialog(project, List.of(gitRepository), GitPushSource.create(localBranch),
                /* isForcePushRequired */ true).show();
            break;
          default :
            break;
        }

        break;

      case BehindRemote :
        boolean pullApproved = true;
        if (PropertiesComponent.getInstance(project).getBoolean(SHOW_PULL_APPROVAL, /* defaultValue */ true)) {
          val pullApprovalDialogBuilder = MessageDialogBuilder.okCancel(
              getString("action.GitMachete.TraverseAction.dialog.pull-verification.title"),
              getString("action.GitMachete.TraverseAction.dialog.pull-verification.text.HTML")
                  .format(gitMacheteBranch.getName(), remoteTrackingBranch.getRemoteName(), remoteTrackingBranch.getName()));

          pullApprovalDialogBuilder
              .yesText(getString("action.GitMachete.TraverseAction.dialog.pull-verification.ok-text"))
              .icon(Messages.getQuestionIcon()).doNotAsk(new PullApprovalDialog(project));

          pullApproved = pullApprovalDialogBuilder.ask(project);
        }

        if (pullApproved) {
          val mergeProps = new MergeProps(
              /* movingBranchName */ gitMacheteBranch,
              /* stayingBranchName */ remoteTrackingBranch);

          val isUpToDate = FetchUpToDateTimeoutStatus.isUpToDate(gitRepository);
          val fetchNotificationPrefix = isUpToDate
                  ? getNonHtmlString("action.GitMachete.BasePullAction.notification.prefix.no-fetch-perform")
                  .format(FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING)
                  : getNonHtmlString("action.GitMachete.BasePullAction.notification.prefix.fetch-perform");
          FastForwardMerge.perform(project, gitRepository, mergeProps,
              fetchNotificationPrefix).queue();
        }
        break;
      default :
        break;
    }
    graphTable.queueRepositoryUpdateAndModelRefresh();
  }

  @UIEffect
  private void syncBranchToParent(@ConfirmedNonRoot IManagedBranchSnapshot gitMacheteBranch,
      AnActionEvent anActionEvent,
      GitRepository gitRepository) {
    val branchName = gitMacheteBranch.getName();

    val remoteTrackingBranch = gitMacheteBranch.getRemoteTrackingBranch();
    val syncToRemoteStatus = gitMacheteBranch.getRelationToRemote().getSyncToRemoteStatus();
    boolean shouldSyncToParent;
    switch (syncToRemoteStatus) {
      case BehindRemote :
      case DivergedFromAndOlderThanRemote :
        shouldSyncToParent = false;
        break;
      default :
        shouldSyncToParent = true;
    }
    val graphTable = getGraphTable(anActionEvent);
    if (shouldSyncToParent) {
      val gitMacheteNonRootBranch = gitMacheteBranch.asNonRoot();
      val syncToParentStatus = gitMacheteNonRootBranch.getSyncToParentStatus();
      val project = getProject(anActionEvent);
      val branchLayout = getBranchLayout(anActionEvent);
      switch (syncToParentStatus) {
        case MergedToParent :
          if (branchLayout != null) {
            new SlideOutBackgroundable(project, "Deleting branch if required...", gitMacheteNonRootBranch.getName(),
                getSelectedGitRepository(anActionEvent), getCurrentBranchNameIfManaged(anActionEvent),
                branchLayout, getBranchLayoutWriter(anActionEvent), graphTable) {
              @Override
              public void onSuccess() {
                super.onSuccess();
                graphTable.queueRepositoryUpdateAndModelRefresh();
                syncBranchToRemote(remoteTrackingBranch, branchName, anActionEvent, gitRepository);
              }
            }.queue();
            return;
          }
          break;
        case InSyncButForkPointOff :
          LOG.debug("Enqueueing fork point override");
          new OverrideForkPointBackgroundable(project, "Overriding fork point...", gitRepository, gitMacheteNonRootBranch,
              graphTable) {
            @Override
            public void onSuccess() {
              super.onSuccess();
              graphTable.queueRepositoryUpdateAndModelRefresh();
              syncBranchToRemote(remoteTrackingBranch, branchName, anActionEvent, gitRepository);
            }
          }.queue();
          return;
        case OutOfSync :
          val nonRootBranch = gitMacheteBranch.asNonRoot();

          val selectedAction = new DivergedFromParentDialog(project, nonRootBranch.getParent(), nonRootBranch)
              .showAndGetThePreferredAction();
          if (selectedAction == null) {
            log().debug(
                "Action selected for resolving divergence from parent is null: most likely the action has been canceled from Diverge-From-Remote-Dialog dialog");
            break;
          }
          switch (selectedAction) {
            case REBASE_ON_PARENT :
              val repositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
              val branchToRebase = gitMacheteBranch.asNonRoot();
              if (repositorySnapshot != null && branchToRebase != null) {
                new RebaseOnParentBackgroundable(project,
                    getString("action.GitMachete.BaseSyncToParentByRebaseAction.hook.task-title"),
                    gitRepository, repositorySnapshot,
                    branchToRebase,
                    /* shouldExplicitlyCheckout */ false) {
                  @Override
                  public void onSuccess() {
                    super.onSuccess();
                    graphTable.queueRepositoryUpdateAndModelRefresh();
                    syncBranchToRemote(remoteTrackingBranch, branchName, anActionEvent, gitRepository);
                  }
                }.queue();
                return;
              }
              break;
            default :
              break;
          }
          break;
        default :
          break;
      }
    }
    graphTable.queueRepositoryUpdateAndModelRefresh();
    syncBranchToRemote(remoteTrackingBranch, branchName, anActionEvent, gitRepository);
  }

}
