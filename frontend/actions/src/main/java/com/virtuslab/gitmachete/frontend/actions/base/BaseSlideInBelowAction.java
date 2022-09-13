package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.ui.branch.GitBranchPopupActions.RemoteBranchActions.CheckoutRemoteBranchAction.checkoutRemoteBranch;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitRemoteBranch;
import git4idea.branch.GitNewBranchDialog;
import git4idea.branch.GitNewBranchOptions;
import git4idea.repo.GitRepository;
import git4idea.ui.branch.GitBranchCheckoutOperation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.SlideInDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public abstract class BaseSlideInBelowAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    final var branchName = getNameOfBranchUnderAction(anActionEvent);
    final var branch = branchName != null
        ? getManagedBranchByName(anActionEvent, branchName)
        : null;

    if (branchName == null) {
      presentation.setVisible(false);
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.BaseSlideInBelowAction.description.disabled.no-parent"));
    } else if (branch == null) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.undefined.machete-branch")
          .format("Slide In", getQuotedStringOrCurrent(branchName)));
    } else {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSlideInBelowAction.description").format(branchName));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    final var project = getProject(anActionEvent);
    final var gitRepository = getSelectedGitRepository(anActionEvent);
    final var parentName = getNameOfBranchUnderAction(anActionEvent);
    final var branchLayout = getBranchLayout(anActionEvent);
    final var branchLayoutWriter = getBranchLayoutWriter(anActionEvent);

    if (gitRepository == null || parentName == null || branchLayout == null) {
      return;
    }

    final var slideInDialog = new SlideInDialog(project, branchLayout, parentName, gitRepository);
    if (!slideInDialog.showAndGet()) {
      log().debug("Options of branch to slide in is null: most likely the action has been canceled from slide-in dialog");
      return;
    }

    final var slideInOptions = slideInDialog.getSlideInOptions();

    if (parentName.equals(slideInOptions.getName())) {
      // @formatter:off
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          /* title */ getString("action.GitMachete.BaseSlideInBelowAction.notification.title.slide-in-fail.HTML").format(slideInOptions.getName()),
          /* message */ getString("action.GitMachete.BaseSlideInBelowAction.notification.message.slide-in-under-itself-or-its-descendant"));
      // @formatter:on
      return;
    }

    final var localBranch = gitRepository.getBranches().findLocalBranch(slideInOptions.getName());
    Runnable preSlideInRunnable = () -> {};
    if (localBranch == null) {
      Tuple2<@Nullable String, Runnable> branchNameAndPreSlideInRunnable = getBranchNameAndPreSlideInRunnable(
          project, gitRepository, parentName, slideInOptions.getName());
      preSlideInRunnable = branchNameAndPreSlideInRunnable._2();
      final var branchName = branchNameAndPreSlideInRunnable._1();
      if (!slideInOptions.getName().equals(branchName)) {
        final var branchNameFromNewBranchDialog = branchName != null ? branchName : "no name provided";
        VcsNotifier.getInstance(project).notifyWeakError(/* displayId */ null,
            /* title */ "",
            getString("action.GitMachete.BaseSlideInBelowAction.notification.message.mismatched-names.HTML")
                .format(slideInOptions.getName(), branchNameFromNewBranchDialog));
        return;
      }
    }

    // TODO (#430): expose getParent from branch layout API
    final var parentEntry = branchLayout.findEntryByName(parentName);
    final var entryAlreadyExistsBelowGivenParent = parentEntry != null
        && parentEntry.getChildren().map(IBranchLayoutEntry::getName)
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
      @Untainted String initialName) {
    final var repositories = java.util.Collections.singletonList(gitRepository);
    final var gitNewBranchDialog = new GitNewBranchDialog(project,
        repositories,
        /* title */ getNonHtmlString("action.GitMachete.BaseSlideInBelowAction.dialog.create-new-branch.title")
            .format(startPoint),
        initialName,
        /* showCheckOutOption */ true,
        /* showResetOption */ true,
        /* showSetTrackingOption */ false);

    GitNewBranchOptions options = gitNewBranchDialog.showAndGetOptions();

    if (options == null) {
      log().debug("Name of branch to slide in is null: " +
          "most likely the action has been canceled from create-new-branch dialog");
      return Tuple.of(null, () -> {});
    }

    final var branchName = options.getName();
    if (!initialName.equals(branchName)) {
      return Tuple.of(branchName, () -> {});
    }

    Runnable preSlideInRunnable = () -> {};
    final var remoteBranch = getGitRemoteBranch(project, gitRepository, branchName);

    if (options.shouldCheckout() && remoteBranch != null) {
      preSlideInRunnable = () -> checkoutRemoteBranch(project, repositories, remoteBranch.getName());

    } else if (!options.shouldCheckout() && remoteBranch != null) {

      final var refspec = createRefspec("refs/remotes/${remoteBranch.getName()}",
          "refs/heads/${branchName}", /* allowNonFastForward */ false);
      preSlideInRunnable = () -> new FetchBackgroundable(
          project,
          gitRepository,
          LOCAL_REPOSITORY_NAME,
          refspec,
          "Fetching Remote Branch",
          getNonHtmlString("action.GitMachete.BasePullAction.notification.title.pull-fail").format(branchName),
          getString("action.GitMachete.BasePullAction.notification.title.pull-success.HTML").format(branchName)).queue();

    } else if (remoteBranch == null) {

      preSlideInRunnable = () -> {
        final var gitBranchCheckoutOperation = new GitBranchCheckoutOperation(project,
            Collections.singletonList(gitRepository));
        gitBranchCheckoutOperation.perform(startPoint, options);
      };
    }

    return Tuple.of(options.getName(), preSlideInRunnable);
  }

  private static @Nullable GitRemoteBranch getGitRemoteBranch(Project project, @NonNull GitRepository gitRepository,
      String branchName) {

    final var remotesWithBranch = List.ofAll(gitRepository.getRemotes())
        .flatMap(r -> {
          final var remoteBranchName = "${r.getName()}/${branchName}";
          GitRemoteBranch remoteBranch = gitRepository.getBranches().findRemoteBranch(remoteBranchName);
          return remoteBranch != null ? Option.some(Tuple.of(r, remoteBranch)) : Option.none();
        })
        // Note: false < true. Hence the pair with origin will be first (head) if exists.
        .sortBy(t -> !t._1().getName().equals("origin"));

    if (remotesWithBranch.isEmpty()) {
      return null;
    }

    final var chosen = remotesWithBranch.head();
    if (remotesWithBranch.size() > 1) {
      final var title = getString("action.GitMachete.BaseSlideInBelowAction.notification.title.multiple-remotes");
      final var message = getString("action.GitMachete.BaseSlideInBelowAction.notification.message.multiple-remotes")
          .format(chosen._2().getName(), chosen._1().getName());
      VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
    }
    return chosen._2();
  }
}
