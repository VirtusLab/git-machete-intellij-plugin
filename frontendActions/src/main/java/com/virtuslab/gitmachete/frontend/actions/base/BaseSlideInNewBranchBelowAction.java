package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;
import static git4idea.ui.branch.GitBranchActionsUtilKt.checkoutOrReset;
import static git4idea.ui.branch.GitBranchActionsUtilKt.createNewBranch;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.branch.GitNewBranchDialog;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseSlideInNewBranchBelowAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeyProject,
      IExpectsKeyGitMacheteRepository,
      IBranchNameProvider {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));

    if (branchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(GitMacheteBundle.message("action.slide-in.description.disabled.no-parent"));
    } else if (branch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(GitMacheteBundle.message("action.description.disabled.undefined.machete.branch", "Slide In"));
    } else {
      presentation.setDescription(GitMacheteBundle.message("action.slide-in.description", branchName.get()));

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(GitMacheteBundle.message("action.slide-in.text"));
      }
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var selectedVcsRepository = getSelectedGitRepository(anActionEvent);
    var gitMacheteParentBranch = getNameOfBranchUnderAction(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));
    var branchLayout = getBranchLayout(anActionEvent);
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);

    if (selectedVcsRepository.isEmpty() || gitMacheteParentBranch.isEmpty() || branchLayout.isEmpty()) {
      return;
    }

    var branchName = createOrCheckoutNewBranch(project, selectedVcsRepository.get(), gitMacheteParentBranch.get().getName(),
        GitMacheteBundle.message("action.slide-in.dialog.title"));
    if (branchName == null) {
      log().debug("Name of branch to slide in is null: most likely the action has been canceled from dialog");
      return;
    }

    new Task.Backgroundable(project, GitMacheteBundle.message("action.slide-in.task.title")) {

      @Override
      public void run(ProgressIndicator indicator) {
        Path macheteFilePath = getMacheteFilePath(selectedVcsRepository.get());
        var notifier = VcsNotifier.getInstance(project);

        var newBranchLayout = Try.of(() -> branchLayout.get().slideIn(gitMacheteParentBranch.get().getName(), branchName))
            .onFailure(
                t -> notifier.notifyError(/* title */ GitMacheteBundle.message("action.slide-in.notification.fail", branchName),
                    getMessageOrEmpty(t)))
            .toOption();

        newBranchLayout.map(nbl -> Try.run(() -> branchLayoutWriter.write(macheteFilePath, nbl, /* backupOldLayout */ true))
            .onFailure(t -> notifier.notifyError(/* title */ GitMacheteBundle.message("branch-layout.write.notification.fail"),
                getMessageOrEmpty(t))));
      }
    }.queue();
  }

  private static String getMessageOrEmpty(Throwable t) {
    return t.getMessage() != null ? t.getMessage() : "";
  }

  @Nullable
  String createOrCheckoutNewBranch(Project project, GitRepository gitRepository, String startPoint, String title) {
    var repositories = List.of(gitRepository).asJava();
    var options = new GitNewBranchDialog(project,
        repositories,
        title,
        /* initialName */ null,
        /* showCheckOutOption */ true,
        /* showResetOption */ true,
        /* localConflictsAllowed */ true).showAndGetOptions();
    if (options == null) {
      return null;
    }

    if (options.shouldCheckout()) {
      checkoutOrReset(project, repositories, startPoint, options);
    } else {
      createNewBranch(project, repositories, startPoint, options);
    }

    return options.getName();
  }
}
