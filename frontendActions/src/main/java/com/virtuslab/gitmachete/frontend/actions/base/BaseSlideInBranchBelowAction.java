package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;
import static git4idea.ui.branch.GitBranchActionsUtilKt.checkoutOrReset;
import static git4idea.ui.branch.GitBranchActionsUtilKt.createNewBranch;
import static java.text.MessageFormat.format;

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
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.gitmachete.frontend.actions.dialogs.SlideInDialog;
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
      presentation.setDescription(getString("action.GitMachete.BaseSlideInBranchBelowAction.description.disabled.no-parent"));
    } else if (branch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.machete-branch"), "Slide In"));
    } else {
      presentation.setDescription(
          format(getString("action.GitMachete.BaseSlideInBranchBelowAction.description"), branchName.get()));

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(getString("action.GitMachete.BaseSlideInBranchBelowAction.text.current-branch"));
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var selectedVcsRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var parentName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    var branchLayout = getBranchLayout(anActionEvent).getOrNull();
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    var notifier = VcsNotifier.getInstance(project);

    if (selectedVcsRepository == null || parentName == null || branchLayout == null) {
      return;
    }

    var slideInOptions = new SlideInDialog(project, branchLayout, parentName).showAndGetBranchName();
    if (slideInOptions == null) {
      log().debug("Options of branch to slide in is null: most likely the action has been canceled from slide-in dialog");
      return;
    }

    if (parentName.equals(slideInOptions.getName())) {
      // @formatter:off
      notifier.notifyError(
          /* title */ format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail"), slideInOptions.getName()),
          /* message */ getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.slide-in-under-itself-or-its-descendant"));
      // @formatter:on
      return;
    }

    var localBranch = selectedVcsRepository.getBranches().findLocalBranch(slideInOptions.getName());
    if (localBranch == null) {
      var createNewBranchDialogBranchName = createOrCheckoutNewBranch(project, selectedVcsRepository, parentName,
          slideInOptions.getName());
      if (!slideInOptions.getName().equals(createNewBranchDialogBranchName)) {
        createNewBranchDialogBranchName = createNewBranchDialogBranchName != null
            ? createNewBranchDialogBranchName
            : "no name provided";
        notifier.notifyWeakError(
            format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail.mismatched-names"),
                slideInOptions.getName(), createNewBranchDialogBranchName));
        return;
      }
    }

    // TODO (#430): expose getParent from branch layout api
    var parentEntry = branchLayout.findEntryByName(parentName);
    var entryAlreadyExistsBelowGivenParent = parentEntry
        .map(entry -> entry.getChildren())
        .map(children -> children.map(e -> e.getName()))
        .map(names -> names.contains(slideInOptions.getName()))
        .getOrElse(false);

    if (entryAlreadyExistsBelowGivenParent) {
      log().debug("Skipping action: Branch layout entry already exists below given parent");
      return;
    }

    new Task.Backgroundable(project, getString("action.GitMachete.BaseSlideInBranchBelowAction.task-title")) {

      @Override
      public void run(ProgressIndicator indicator) {
        Path macheteFilePath = getMacheteFilePath(selectedVcsRepository);

        var childEntryByName = branchLayout.findEntryByName(slideInOptions.getName());
        IBranchLayoutEntry entryToSlideIn;
        IBranchLayout targetBranchLayout;
        if (childEntryByName.isDefined()) {

          if (slideInOptions.shouldReattach()) {
            entryToSlideIn = childEntryByName.get();
            targetBranchLayout = branchLayout;
          } else {
            entryToSlideIn = childEntryByName.map(e -> e.withChildren(List.empty())).getOrNull();
            targetBranchLayout = Try.of(() -> branchLayout.slideOut(slideInOptions.getName()))
                .onFailure(
                    t -> notifier.notifyError(
                        /* title */ format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail"),
                            slideInOptions.getName()),
                        getMessageOrEmpty(t)))
                .getOrNull();

            if (targetBranchLayout == null) {
              return;
            }
          }

        } else {
          entryToSlideIn = new BranchLayoutEntry(slideInOptions.getName(), /* customAnnotation */ null,
              /* children */ List.empty());
          targetBranchLayout = branchLayout;
        }

        var newBranchLayout = Try
            .of(() -> targetBranchLayout.slideIn(parentName, entryToSlideIn))
            .onFailure(
                t -> notifier.notifyError(
                    /* title */ format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail"),
                        slideInOptions.getName()),
                    getMessageOrEmpty(t)))
            .toOption();

        newBranchLayout.map(nbl -> Try.run(() -> branchLayoutWriter.write(macheteFilePath, nbl, /* backupOldLayout */ true))
            .onFailure(t -> notifier.notifyError(
                /* title */ getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.fail.branch-layout-write"),
                getMessageOrEmpty(t))));
      }

      @Override
      public void onFinished() {
        getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      }
    }.queue();
  }

  private static String getMessageOrEmpty(Throwable t) {
    return t.getMessage() != null ? t.getMessage() : "";
  }

  @Nullable
  String createOrCheckoutNewBranch(Project project, GitRepository gitRepository, String startPoint, String initialName) {
    var repositories = java.util.Collections.singletonList(gitRepository);
    var gitNewBranchDialog = new GitNewBranchDialog(project,
        repositories,
        /* title */ format(getString("action.GitMachete.BaseSlideInBranchBelowAction.dialog.create-new-branch.title"),
            startPoint),
        initialName,
        /* showCheckOutOption */ true,
        /* showResetOption */ true,
        /* localConflictsAllowed */ true);

    var options = gitNewBranchDialog.showAndGetOptions();

    if (options == null) {
      log().debug(
          "Name of branch to slide in is null: most likely the action has been canceled from create-new-branch dialog");
      return null;
    }

    var branchName = options.getName();
    if (!initialName.equals(branchName)) {
      return branchName;
    }

    if (options.shouldCheckout()) {
      checkoutOrReset(project, repositories, startPoint, options);
    } else {
      createNewBranch(project, repositories, startPoint, options);
    }

    return options.getName();
  }
}
