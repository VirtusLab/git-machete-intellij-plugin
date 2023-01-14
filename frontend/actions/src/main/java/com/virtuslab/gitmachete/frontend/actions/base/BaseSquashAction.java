package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Collections;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.vcs.log.VcsCommitMetadata;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.rebase.log.GitCommitEditingOperationResult;
import git4idea.rebase.log.squash.GitSquashOperation;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import kotlin.Unit;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SideEffectingBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.VcsCommitMetadataAdapterForSquash;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitNewCommitMessageActionDialog;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
public abstract class BaseSquashAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider {

  private final String NL = System.lineSeparator();

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val managedBranch = getManagedBranchByName(anActionEvent, branchName);
    val nonRootBranch = managedBranch != null && managedBranch.isNonRoot()
        ? managedBranch.asNonRoot()
        : null;
    val syncToParentStatus = nonRootBranch != null ? nonRootBranch.getSyncToParentStatus() : null;

    if (branchName != null) {
      if (nonRootBranch == null) {
        presentation.setDescription(
            getNonHtmlString("action.GitMachete.BaseSquashAction.branch-is-root").fmt(branchName));
        presentation.setEnabled(false);

      } else {
        val numberOfCommits = nonRootBranch.getUniqueCommits().length();

        val description = getNonHtmlString("action.GitMachete.BaseSquashAction.not-enough-commits")
            .fmt(branchName, numberOfCommits + "", numberOfCommits == 1 ? "" : "s");

        if (numberOfCommits < 2) {
          presentation.setDescription(description);
          presentation.setEnabled(false);
        } else if (syncToParentStatus == InSyncButForkPointOff) {
          presentation.setEnabled(false);
          presentation.setDescription(getNonHtmlString("action.GitMachete.BaseSquashAction.fork-point-off")
              .fmt(branchName));
        } else {
          val currentBranchIfManaged = getCurrentBranchNameIfManaged(anActionEvent);
          val isSquashingCurrentBranch = currentBranchIfManaged != null && currentBranchIfManaged.equals(branchName);
          if (anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU) && isSquashingCurrentBranch) {
            presentation.setText(getString("action.GitMachete.BaseSquashAction.text"));
          }
        }
      }
    }
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val managedBranch = getManagedBranchByName(anActionEvent, branchName);
    val nonRootBranch = managedBranch != null && managedBranch.isNonRoot()
        ? managedBranch.asNonRoot()
        : null;
    if (nonRootBranch != null) {
      val commits = nonRootBranch.getUniqueCommits();
      val parent = nonRootBranch.getForkPoint();
      val syncToParentStatus = nonRootBranch.getSyncToParentStatus();
      val gitRepository = getSelectedGitRepository(anActionEvent);

      if (commits != null && gitRepository != null && parent != null && branchName != null
          && syncToParentStatus != InSyncButForkPointOff) {
        val currentBranch = gitRepository.getCurrentBranch();
        val isSquashingCurrentBranch = currentBranch != null && branchName.equals(currentBranch.getName());
        doSquash(gitRepository, parent, commits, branchName, isSquashingCurrentBranch);
      }
    }
  }

  @Data
  // So that Interning Checker doesn't complain about enum comparison (by `equals` and not by `==`) in Lombok-generated `equals`
  @SuppressWarnings("interning:not.interned")
  private static class VcsCommitMetadataAndMessage {
    private final List<VcsCommitMetadata> metadata;
    private final String message;
  }

  @ContinuesInBackground
  @UIEffect
  private void doSquash(
      GitRepository gitRepository,
      ICommitOfManagedBranch parent,
      List<ICommitOfManagedBranch> commits,
      String branchName,
      boolean isSquashingCurrentBranch) {
    val project = gitRepository.getProject();

    val vcsCommitMetadataAndMessage = commits.foldLeft(
        new VcsCommitMetadataAndMessage(List.empty(), ""),
        (acc, commit) -> new VcsCommitMetadataAndMessage(
            acc.metadata.append(new VcsCommitMetadataAdapterForSquash(parent, commit)),
            "${commit.getFullMessage()}${NL}${NL}${acc.message}"));

    val dialog = new GitNewCommitMessageActionDialog(
        /* project */ project,
        /* message */ vcsCommitMetadataAndMessage.message,
        /* title */ getNonHtmlString("action.GitMachete.BaseSquashAction.dialog.title"),
        /* dialogLabel */ getNonHtmlString("action.GitMachete.BaseSquashAction.dialog.label"));

    String taskName = isSquashingCurrentBranch
        ? getString("action.GitMachete.BaseSquashAction.task-title.current")
        : getString("action.GitMachete.BaseSquashAction.task-title.non-current");

    dialog.show(
        newMessage -> {
          new SideEffectingBackgroundable(project, taskName, "squash") {
            @Override
            @UIThreadUnsafe
            public void doRun(ProgressIndicator indicator) {
              log().info("Checking out '${branchName}' branch and squashing it");

              if (!isSquashingCurrentBranch) {
                val uiHandler = new GitBranchUiHandlerImpl(project, indicator);
                new GitBranchWorker(project, Git.getInstance(), uiHandler)
                    .checkout(/* reference */ branchName, /* detach */ false, Collections.singletonList(gitRepository));
              }

              val commitsToSquash = vcsCommitMetadataAndMessage.metadata.toJavaList();
              val operationResult = new GitSquashOperation(gitRepository).execute(commitsToSquash, newMessage);

              if (isComplete(operationResult)) {
                val title = getString("action.GitMachete.BaseSquashAction.notification.title");
                val notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(title, NotificationType.INFORMATION);
                VcsNotifier.getInstance(project).notify(notification);
              }
            }
          }.queue();

          return Unit.INSTANCE;
        });
  }

  /**
   * Hackish approach to check for kotlin internal sealed class {@link git4idea.rebase.log.GitCommitEditingOperationResult.Complete}.
    */
  private boolean isComplete(GitCommitEditingOperationResult operationResult) {
    return operationResult.toString().contains("Complete");
  }
}
