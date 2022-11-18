package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.ui.branch.GitBranchPopupActions.RemoteBranchActions.CheckoutRemoteBranchAction.checkoutRemoteBranch;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
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
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInNonRootBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.SlideInDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;

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

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName != null
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
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val parentName = getNameOfBranchUnderAction(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);
    val branchLayoutWriter = getBranchLayoutWriter();

    if (gitRepository == null || parentName == null || branchLayout == null) {
      return;
    }

    val slideInDialog = new SlideInDialog(project, branchLayout, parentName, gitRepository);
    if (!slideInDialog.showAndGet()) {
      log().debug("Options of branch to slide in is null: most likely the action has been canceled from slide-in dialog");
      return;
    }

    val slideInOptions = slideInDialog.getSlideInOptions();
    String slideInOptionsName = slideInOptions.getName();

    if (parentName.equals(slideInOptionsName)) {
      // @formatter:off
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          /* title */ getString("action.GitMachete.BaseSlideInBelowAction.notification.title.slide-in-fail.HTML")
                      .format(escapeHtml4(slideInOptionsName)),
          /* message */ getString("action.GitMachete.BaseSlideInBelowAction.notification.message.slide-in-under-itself-or-its-descendant"));
      // @formatter:on
      return;
    }

    Runnable preSlideInRunnable = () -> {};
    val localBranch = gitRepository.getBranches().findLocalBranch(slideInOptionsName);

    if (localBranch == null) {
      Tuple2<@Nullable String, Runnable> branchNameAndPreSlideInRunnable = getBranchNameAndPreSlideInRunnable(
          project, gitRepository, parentName, slideInOptionsName);

      preSlideInRunnable = branchNameAndPreSlideInRunnable._2();
      val branchName = branchNameAndPreSlideInRunnable._1();

      if (!slideInOptionsName.equals(branchName)) {
        val branchNameFromNewBranchDialog = branchName != null ? branchName : "no name provided";
        VcsNotifier.getInstance(project).notifyWeakError(/* displayId */ null,
            /* title */ "",
            getString("action.GitMachete.BaseSlideInBelowAction.notification.message.mismatched-names.HTML")
                .format(escapeHtml4(slideInOptionsName), escapeHtml4(branchNameFromNewBranchDialog)));
        return;
      }
    }

    val parentEntry = branchLayout.getEntryByName(parentName);
    val entryAlreadyExistsBelowGivenParent = parentEntry != null
        && parentEntry.getChildren().map(BranchLayoutEntry::getName)
            .map(names -> names.contains(slideInOptionsName))
            .getOrElse(false);

    if (entryAlreadyExistsBelowGivenParent && slideInOptions.shouldReattach()) {
      log().debug("Skipping action: Branch layout entry already exists below given parent");
      return;
    }

    new SlideInNonRootBackgroundable(project,
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

  // The UI thread-unsafe calls are actually happening within Runnable lambdas
  // which are going to be executed outside of UI thread.
  @IgnoreUIThreadUnsafeCalls({
      "git4idea.ui.branch.GitBranchCheckoutOperation.<init>" +
          "(com.intellij.openapi.project.Project, java.util.List)",
      "git4idea.ui.branch.GitBranchCheckoutOperation.perform" +
          "(java.lang.String, git4idea.branch.GitNewBranchOptions)",
      "git4idea.ui.branch.GitBranchPopupActions$RemoteBranchActions$CheckoutRemoteBranchAction.checkoutRemoteBranch" +
          "(com.intellij.openapi.project.Project, java.util.List, java.lang.String)"
  })
  Tuple2<@Nullable String, Runnable> getBranchNameAndPreSlideInRunnable(
      Project project,
      GitRepository gitRepository,
      String startPoint,
      String initialName) {
    val repositories = java.util.Collections.singletonList(gitRepository);
    val gitNewBranchDialog = new GitNewBranchDialog(project,
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

    val branchName = options.getName();
    if (!initialName.equals(branchName)) {
      return Tuple.of(branchName, () -> {});
    }

    Runnable preSlideInRunnable = () -> {};
    val remoteBranch = getGitRemoteBranch(project, gitRepository, branchName);

    if (options.shouldCheckout() && remoteBranch != null) {
      preSlideInRunnable = () -> checkoutRemoteBranch(project, repositories, remoteBranch.getName());

    } else if (!options.shouldCheckout() && remoteBranch != null) {

      val refspec = createRefspec("refs/remotes/${remoteBranch.getName()}",
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
        val gitBranchCheckoutOperation = new GitBranchCheckoutOperation(project, Collections.singletonList(gitRepository));
        gitBranchCheckoutOperation.perform(startPoint, options);
      };
    }

    return Tuple.of(options.getName(), preSlideInRunnable);
  }

  private static @Nullable GitRemoteBranch getGitRemoteBranch(Project project, GitRepository gitRepository, String branchName) {

    val remotesWithBranch = List.ofAll(gitRepository.getRemotes())
        .flatMap(r -> {
          val remoteBranchName = "${r.getName()}/${branchName}";
          GitRemoteBranch remoteBranch = gitRepository.getBranches().findRemoteBranch(remoteBranchName);
          return remoteBranch != null ? Option.some(Tuple.of(r, remoteBranch)) : Option.none();
        })
        // Note: false < true. Hence the pair with origin will be first (head) if exists.
        .sortBy(t -> !t._1().getName().equals("origin"));

    if (remotesWithBranch.isEmpty()) {
      return null;
    }

    val chosen = remotesWithBranch.head();
    if (remotesWithBranch.size() > 1) {
      val title = getString("action.GitMachete.BaseSlideInBelowAction.notification.title.multiple-remotes");
      val message = getString("action.GitMachete.BaseSlideInBelowAction.notification.message.multiple-remotes")
          .format(chosen._2().getName(), chosen._1().getName());
      VcsNotifier.getInstance(project).notifyInfo(/* displayId */ null, title, message);
    }
    return chosen._2();
  }
}
