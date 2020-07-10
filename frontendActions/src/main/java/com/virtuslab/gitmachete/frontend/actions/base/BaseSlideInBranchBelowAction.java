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

import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseSlideInBranchBelowAction extends BaseGitMacheteRepositoryReadyAction
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
      presentation.setDescription(
          GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.description.disabled.no-parent"));
    } else if (branch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          GitMacheteBundle.message("action.GitMachete.description.disabled.undefined.machete-branch", "Slide In"));
    } else {
      presentation.setDescription(
          GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.description", branchName.get()));

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.text.current-branch"));
      }
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var selectedVcsRepository = getSelectedGitRepository(anActionEvent);
    var gitMacheteParentBranch = getNameOfBranchUnderAction(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));
    var branchLayout = getBranchLayout(anActionEvent).getOrNull();
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);

    if (selectedVcsRepository.isEmpty() || gitMacheteParentBranch.isEmpty() || branchLayout == null) {
      return;
    }

    var parentName = gitMacheteParentBranch.get().getName();
    var branchName = createOrCheckoutNewBranch(project, selectedVcsRepository.get(), parentName,
        GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.title", parentName));
    if (branchName == null) {
      log().debug("Name of branch to slide in is null: most likely the action has been canceled from dialog");
      return;
    }

    if (parentName.equals(branchName)) {
      VcsNotifier.getInstance(project).notifyError(
          /* title */ GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail",
              branchName),
          /* message */ GitMacheteBundle
              .message("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.branch-name-equals-parent"));
      return;
    }

    // TODO (#430): expose getParent from branch layout api
    var parentEntry = branchLayout.findEntryByName(parentName);
    var entryAlreadyExistsBelowGivenParent = parentEntry
        .map(entry -> entry.getChildren())
        .map(children -> children.map(e -> e.getName()))
        .map(names -> names.contains(branchName))
        .getOrElse(false);

    if (entryAlreadyExistsBelowGivenParent) {
      log().debug("Skipping action: Branch layout entry already exists below given parent");
      return;
    }

    var entryToSlideIn = branchLayout.findEntryByName(branchName)
        .getOrElse(new BranchLayoutEntry(branchName, /* customAnnotation */ null, List.empty()));

    new Task.Backgroundable(project, GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.task-title")) {

      @Override
      public void run(ProgressIndicator indicator) {
        Path macheteFilePath = getMacheteFilePath(selectedVcsRepository.get());
        var notifier = VcsNotifier.getInstance(project);

        var newBranchLayout = Try
            .of(() -> branchLayout.slideIn(parentName, entryToSlideIn))
            .onFailure(
                t -> notifier.notifyError(
                    /* title */ GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail",
                        branchName),
                    getMessageOrEmpty(t)))
            .toOption();

        newBranchLayout.map(nbl -> Try.run(() -> branchLayoutWriter.write(macheteFilePath, nbl, /* backupOldLayout */ true))
            .onFailure(t -> notifier.notifyError(
                /* title */ GitMacheteBundle
                    .message("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail.branch-layout-write"),
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
    var gitNewBranchDialog = new GitNewBranchDialog(project,
        repositories,
        title,
        /* initialName */ null,
        /* showCheckOutOption */ true,
        /* showResetOption */ true,
        /* localConflictsAllowed */ true);

    var options = gitNewBranchDialog.showAndGetOptions();
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
