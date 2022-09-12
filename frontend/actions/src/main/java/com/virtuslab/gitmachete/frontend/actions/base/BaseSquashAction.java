package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.vcs.log.VcsCommitMetadata;
import git4idea.rebase.log.GitCommitEditingOperationResult;
import git4idea.rebase.log.squash.GitSquashOperation;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import kotlin.Unit;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.frontend.actions.common.VcsCommitMetadataAdapterForSquash;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitNewCommitMessageActionDialog;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public abstract class BaseSquashAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider {

  private final String NL = System.lineSeparator();

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();

    final var branchName = getNameOfBranchUnderAction(anActionEvent);
    final var managedBranch = getManagedBranchByName(anActionEvent, branchName);
    final var nonRootBranch = managedBranch != null && managedBranch.isNonRoot()
        ? managedBranch.asNonRoot()
        : null;
    final var syncToParentStatus = nonRootBranch != null ? nonRootBranch.getSyncToParentStatus() : null;

    if (branchName != null && nonRootBranch == null) {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSquashAction.branch-is-root").format(branchName));
      presentation.setEnabled(false);

    } else if (branchName != null && nonRootBranch != null) { // nonRootBranch is not null here.
      final var numberOfCommits = nonRootBranch.getCommits().length();

      final var description = getNonHtmlString("action.GitMachete.BaseSquashAction.not-enough-commits") //format is a static method, so it should not be called on instance.
          // Now what is the role of getNonHtmlString?
          .format(branchName, numberOfCommits + "", numberOfCommits == 1 ? "" : "s");

      if (numberOfCommits < 2) {
        presentation.setDescription(description);
        presentation.setEnabled(false);
      } else if (syncToParentStatus == InSyncButForkPointOff) {
        presentation.setEnabled(false);
        presentation.setDescription(getNonHtmlString("action.GitMachete.BaseSquashAction.fork-point-off")
            .format(branchName));
      } else {
        final var currentBranchIfManaged = getCurrentBranchNameIfManaged(anActionEvent);
        final var isSquashingCurrentBranch = currentBranchIfManaged != null && currentBranchIfManaged.equals(branchName);
        if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU) && isSquashingCurrentBranch) {
          presentation.setText(getString("action.GitMachete.BaseSquashAction.text"));
        }
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    final var project = getProject(anActionEvent);
    final var branchName = getNameOfBranchUnderAction(anActionEvent);
    final var managedBranch = getManagedBranchByName(anActionEvent, branchName);
    final var nonRootBranch = managedBranch != null && managedBranch.isNonRoot()
        ? managedBranch.asNonRoot()
        : null;
    if (nonRootBranch != null) {
      final var commits = nonRootBranch.getCommits();
      final var parent = nonRootBranch.getForkPoint();
      final var syncToParentStatus = nonRootBranch.getSyncToParentStatus();
      final var gitRepository = getSelectedGitRepository(anActionEvent);

      if (commits != null && gitRepository != null && parent != null && branchName != null
          && syncToParentStatus != InSyncButForkPointOff) {
        final var currentBranch = gitRepository.getCurrentBranch();
        final var isSquashingCurrentBranch = currentBranch != null && branchName.equals(currentBranch.getName());
        doSquash(project, gitRepository, parent, commits, branchName, isSquashingCurrentBranch);
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

  @UIEffect
  private void doSquash(Project project,
      GitRepository gitRepository,
      ICommitOfManagedBranch parent,
      List<ICommitOfManagedBranch> commits,
      String branchName,
      boolean isSquashingCurrentBranch) {

    BaseSquashAction.VcsCommitMetadataAndMessage vcsCommitMetadataAndMessage = commits.foldLeft(
        new VcsCommitMetadataAndMessage(List.empty(), ""),
        (acc, commit) -> new VcsCommitMetadataAndMessage(
            acc.metadata.append(new VcsCommitMetadataAdapterForSquash(parent, commit)),
            "${commit.getFullMessage()}${NL}${NL}${acc.message}"));

    final var dialog = new GitNewCommitMessageActionDialog(
        /* project */ project,
        /* message */ vcsCommitMetadataAndMessage.message,
        /* title */ getNonHtmlString("action.GitMachete.BaseSquashAction.dialog.title"),
        /* dialogLabel */ getNonHtmlString("action.GitMachete.BaseSquashAction.dialog.label"));

    String taskName = isSquashingCurrentBranch
        ? getString("action.GitMachete.BaseSquashAction.task-title.current")
        : getString("action.GitMachete.BaseSquashAction.task-title.non-current");

    dialog.show(
        newMessage -> {
          new Task.Backgroundable(project, taskName) {
            @Override
            @UIThreadUnsafe
            public void run(ProgressIndicator indicator) {
              LOG.info("Checking out '${branchName}' branch and squashing it");

              if (!isSquashingCurrentBranch) {
                CheckoutSelectedAction.doCheckout(project, indicator, branchName, gitRepository);
              }

              final var commitsToSquash = vcsCommitMetadataAndMessage.metadata.toJavaList();
              final var operationResult = new GitSquashOperation(gitRepository).execute(commitsToSquash, newMessage);

              if (isComplete(operationResult)) {
                final var title = getString("action.GitMachete.BaseSquashAction.notification.title");
                final var notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(title,
                    NotificationType.INFORMATION);
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
