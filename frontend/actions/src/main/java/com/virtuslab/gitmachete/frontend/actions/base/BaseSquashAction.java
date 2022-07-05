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
import git4idea.rebase.log.squash.GitSquashOperation;
import git4idea.repo.GitRepository;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import kotlin.Unit;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.frontend.actions.common.VcsCommitMetadataAdapter;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedBranchAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitNewCommitMessageActionDialog;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public abstract class BaseSquashAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider {

  private final String LS = System.lineSeparator();

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val nonRootBranchOption = branchName.flatMap(bn -> getManagedBranchByName(anActionEvent, bn))
        .filter(b -> b.isNonRoot())
        .map(b -> {
          assert b.isNonRoot() : "branch is root";
          return b.asNonRoot();
        });
    val syncToParentStatus = nonRootBranchOption.map(b -> b.getSyncToParentStatus()).getOrNull();
    val numberOfCommitsOption = nonRootBranchOption.map(b -> b.getCommits().length());

    if (branchName.isDefined() && nonRootBranchOption.isEmpty()) {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSquashAction.branch-is-root")
              .format(branchName.get()));
      presentation.setEnabled(false);

    } else if (branchName.isDefined() && numberOfCommitsOption.isDefined()) {

      val numberOfCommits = numberOfCommitsOption.getOrNull();
      assert numberOfCommits != null : "unknown number of commits";

      if (numberOfCommits < 2) {
        presentation.setDescription(
            getNonHtmlString("action.GitMachete.BaseSquashAction.not-enough-commits")
                .format(branchName.get(), numberOfCommits.toString()));
        presentation.setEnabled(false);
      } else if (syncToParentStatus == InSyncButForkPointOff) {
        presentation.setEnabled(false);
        presentation.setDescription(getNonHtmlString("action.GitMachete.BaseSquashAction.fork-point-off")
            .format(branchName.get()));
      } else {
        val isSquashingCurrent = getCurrentBranchNameIfManaged(anActionEvent)
            .map(bn -> bn.equals(branchName.get())).getOrElse(false);
        if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU) && isSquashingCurrent) {
          presentation.setText(getString("action.GitMachete.BaseSquashAction.text"));
        }
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    val project = getProject(anActionEvent);
    val branchNameOption = getNameOfBranchUnderAction(anActionEvent);
    val nonRootBranchOption = branchNameOption.flatMap(bn -> getManagedBranchByName(anActionEvent, bn))
        .filter(b -> b.isNonRoot())
        .map(b -> {
          assert b.isNonRoot() : "branch is root";
          return b.asNonRoot();
        });
    val commitsOption = nonRootBranchOption
        .map(b -> b.getCommits());
    val parent = nonRootBranchOption
        .flatMap(b -> b.getForkPoint()).getOrNull();
    val syncToParentStatus = nonRootBranchOption.map(b -> b.getSyncToParentStatus()).getOrNull();
    val gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    val branchName = branchNameOption.getOrNull();
    val commits = commitsOption.getOrNull();

    if (commits != null && gitRepository != null && parent != null && branchName != null
        && syncToParentStatus != InSyncButForkPointOff) {
      val isSquashingCurrent = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName())
          .flatMap(cbn -> branchNameOption.map(bn -> bn.equals(cbn))).getOrElse(false);
      doSquash(project, gitRepository, parent, commits, branchName, isSquashingCurrent);
    }
  }

  @UIEffect
  private void doSquash(Project project,
      GitRepository gitRepository,
      ICommitOfManagedBranch parent,
      List<ICommitOfManagedBranch> commits,
      String branchName,
      boolean isSquashingCurrent) {
    val vcsCommitMetadataAndMessage = commits.foldLeft(
        new Tuple2<List<VcsCommitMetadata>, String>(List.empty(), ""),
        (acc, commit) -> new Tuple2<>(
            acc._1.append(new VcsCommitMetadataAdapter(parent, commit)), "${acc._2}${commit.getFullMessage()}${LS}${LS}"));

    val dialog = new GitNewCommitMessageActionDialog(
        /* project */ project,
        /* message */ vcsCommitMetadataAndMessage._2,
        /* title */ getNonHtmlString("action.GitMachete.BaseSquashAction.dialog.title"),
        /* dialogLabel */ getNonHtmlString("action.GitMachete.BaseSquashAction.dialog.label"));

    String taskName = isSquashingCurrent
        ? getString("action.GitMachete.BaseSquashAction.task-title.current")
        : getString("action.GitMachete.BaseSquashAction.task-title.non-current");

    dialog.show(
        newMessage -> {
          new Task.Backgroundable(project, taskName) {
            @Override
            @UIThreadUnsafe
            public void run(ProgressIndicator indicator) {
              LOG.info("Checking out '${branchName}' branch and squashing it");

              if (!isSquashingCurrent) {
                CheckoutSelectedBranchAction.doCheckout(project, indicator, branchName, gitRepository);
              }

              val commitsToSquash = vcsCommitMetadataAndMessage._1.toJavaList();
              val operationResult = new GitSquashOperation(gitRepository).execute(commitsToSquash, newMessage);

              // Hackish approach to check for internal sealed class git4idea.rebase.log.GitCommitEditingOperationResult.Complete.
              if (operationResult.toString().contains("Complete")) {
                val title = getString("action.GitMachete.BaseSquashAction.notification.title");
                val notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(title, NotificationType.INFORMATION);
                VcsNotifier.getInstance(project).notify(notification);
              }
            }
          }.queue();

          return Unit.INSTANCE;
        });
  }
}
