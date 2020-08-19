package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.ui.branch.GitBranchActionsUtilKt.checkoutOrReset;
import static git4idea.ui.branch.GitBranchActionsUtilKt.createNewBranch;
import static git4idea.ui.branch.GitBranchPopupActions.RemoteBranchActions.CheckoutRemoteBranchAction.checkoutRemoteBranch;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitRemoteBranch;
import git4idea.branch.GitNewBranchDialog;
import git4idea.branch.GitNewBranchOptions;
import git4idea.repo.GitRepository;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.SlideInDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseSlideInBranchBelowAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProviderWithLogging,
      IBranchNameProviderWithoutLogging,
      IExpectsKeyGitMacheteRepository {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderActionWithoutLogging(anActionEvent).getOrNull();
    var branch = branchName != null
        ? getGitMacheteBranchByNameWithoutLogging(anActionEvent, branchName).getOrNull()
        : null;

    if (branchName == null) {
      presentation.setEnabled(false);
      presentation.setDescription(getString("action.GitMachete.BaseSlideInBranchBelowAction.description.disabled.no-parent"));
    } else if (branch == null) {
      presentation.setEnabled(false);
      presentation.setDescription(format(getString("action.GitMachete.description.disabled.undefined.machete-branch"),
          "Slide In", getQuotedStringOrCurrent(branchName)));
    } else {
      presentation.setDescription(
          format(getString("action.GitMachete.BaseSlideInBranchBelowAction.description"), branchName));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepositoryWithLogging(anActionEvent).getOrNull();
    var parentName = getNameOfBranchUnderActionWithLogging(anActionEvent).getOrNull();
    var branchLayout = getBranchLayoutWithLogging(anActionEvent).getOrNull();
    var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);
    var notifier = VcsNotifier.getInstance(project);

    if (gitRepository == null || parentName == null || branchLayout == null) {
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
          /* title */ format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.title.slide-in-fail"), slideInOptions.getName()),
          /* message */ getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.slide-in-under-itself-or-its-descendant"));
      // @formatter:on
      return;
    }

    var localBranch = gitRepository.getBranches().findLocalBranch(slideInOptions.getName());
    Runnable preSlideInRunnable = () -> {};
    if (localBranch == null) {
      Tuple2<@Nullable String, Runnable> branchNameAndPreSlideInRunnable = getBranchNameAndPreSlideInRunnable(project,
          gitRepository, parentName, slideInOptions.getName());
      preSlideInRunnable = branchNameAndPreSlideInRunnable._2();
      var branchName = branchNameAndPreSlideInRunnable._1();
      if (!slideInOptions.getName().equals(branchName)) {
        var branchNameFromNewBranchDialog = branchName != null ? branchName : "no name provided";
        notifier.notifyWeakError(
            format(getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.mismatched-names"),
                slideInOptions.getName(), branchNameFromNewBranchDialog));
        return;
      }
    }

    // TODO (#430): expose getParent from branch layout API
    var parentEntry = branchLayout.findEntryByName(parentName);
    var entryAlreadyExistsBelowGivenParent = parentEntry
        .map(entry -> entry.getChildren())
        .map(children -> children.map(e -> e.getName()))
        .map(names -> names.contains(slideInOptions.getName()))
        .getOrElse(false);

    if (entryAlreadyExistsBelowGivenParent && slideInOptions.shouldReattach()) {
      log().debug("Skipping action: Branch layout entry already exists below given parent");
      return;
    }

    new SlideInBackgroundable(project,
        gitRepository,
        branchLayout,
        branchLayoutWriter,
        preSlideInRunnable,
        slideInOptions,
        parentName) {
      @Override
      public void onFinished() {
        getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
      }
    }.queue();
  }

  Tuple2<@Nullable String, Runnable> getBranchNameAndPreSlideInRunnable(
      Project project,
      GitRepository gitRepository,
      String startPoint,
      String initialName) {
    var repositories = java.util.Collections.singletonList(gitRepository);
    var gitNewBranchDialog = new GitNewBranchDialog(project,
        repositories,
        /* title */ format(getString("action.GitMachete.BaseSlideInBranchBelowAction.dialog.create-new-branch.title"),
            startPoint),
        initialName,
        /* showCheckOutOption */ true,
        /* showResetOption */ true,
        /* showSetTrackingOption */ false); // TODO (#496): use setTrackingOption that comes with 2020.2

    GitNewBranchOptions options = gitNewBranchDialog.showAndGetOptions();

    if (options == null) {
      log().debug("Name of branch to slide in is null: " +
          "most likely the action has been canceled from create-new-branch dialog");
      return Tuple.of(null, () -> {});
    }

    var branchName = options.getName();
    if (!initialName.equals(branchName)) {
      return Tuple.of(branchName, () -> {});
    }

    Runnable preSlideInRunnable = () -> {};
    var remoteBranch = getGitRemoteBranch(project, gitRepository, branchName);

    if (options.shouldCheckout() && remoteBranch != null) {
      preSlideInRunnable = () -> checkoutRemoteBranch(project, repositories, remoteBranch.getName());

    } else if (!options.shouldCheckout() && remoteBranch != null) {

      var refspec = createRefspec("refs/remotes/${remoteBranch.getName()}",
          "refs/heads/${branchName}", /* allowNonFastForward */ false);
      preSlideInRunnable = () -> new FetchBackgroundable(project, gitRepository, LOCAL_REPOSITORY_NAME, refspec,
          "Fetching Remote Branch").queue();

    } else if (options.shouldCheckout() && remoteBranch == null) {
      preSlideInRunnable = () -> checkoutOrReset(project, repositories, startPoint, options);

    } else if (!options.shouldCheckout() && remoteBranch == null) {
      preSlideInRunnable = () -> createNewBranch(project, repositories, startPoint, options);
    }

    return Tuple.of(options.getName(), preSlideInRunnable);
  }

  private static @Nullable GitRemoteBranch getGitRemoteBranch(Project project, GitRepository gitRepository, String branchName) {

    var remotesWithBranch = List.ofAll(gitRepository.getRemotes())
        .flatMap(r -> {
          var remoteBranchName = "${r.getName()}/${branchName}";
          var remoteBranch = gitRepository.getBranches().findRemoteBranch(remoteBranchName);
          return remoteBranch != null ? Option.some(Tuple.of(r, remoteBranch)) : Option.none();
        })
        // Note: false < true. Hence the pair with origin will be first (head) if exists.
        .sortBy(t -> !t._1().getName().equals("origin"));

    if (remotesWithBranch.isEmpty()) {
      return null;
    }

    var chosen = remotesWithBranch.head();
    if (remotesWithBranch.size() > 1) {
      VcsNotifier.getInstance(project).notifyInfo(format(
          getString("action.GitMachete.BaseSlideInBranchBelowAction.notification.message.multiple-remotes"),
          chosen._2().getName(), chosen._1().getName()));
    }
    return chosen._2();
  }
}
