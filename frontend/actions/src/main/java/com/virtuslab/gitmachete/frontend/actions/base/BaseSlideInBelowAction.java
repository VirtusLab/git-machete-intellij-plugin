package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable.LOCAL_REPOSITORY_NAME;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitRemoteBranch;
import git4idea.branch.GitNewBranchOptions;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.ui.branch.GitBranchCheckoutOperation;
import git4idea.ui.branch.GitBranchPopupActions.RemoteBranchActions;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Value;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInNonRootBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.UiThreadUnsafeRunnable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.MyGitNewBranchDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.SlideInDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
public abstract class BaseSlideInBelowAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {

  @Override
  protected boolean isSideEffecting() {
    return true;
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
          .fmt("Slide In", getQuotedStringOrCurrent(branchName)));
    } else {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSlideInBelowAction.description").fmt(branchName));
    }
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val parentName = getNameOfBranchUnderAction(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);
    val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);

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
              /* title */ getString("action.GitMachete.BaseSlideInBelowAction.notification.title.slide-in-fail.HTML").fmt(escapeHtml4(slideInOptionsName)),
              /* message */ getString("action.GitMachete.BaseSlideInBelowAction.notification.message.slide-in-under-itself-or-its-descendant"));
      // @formatter:on
      return;
    }

    UiThreadUnsafeRunnable preSlideInRunnable = () -> {};
    val localBranch = gitRepository.getBranches().findLocalBranch(slideInOptionsName);

    if (localBranch == null) {
      Tuple2<@Nullable String, UiThreadUnsafeRunnable> branchNameAndPreSlideInRunnable = getBranchNameAndPreSlideInRunnable(
          gitRepository,
          parentName, slideInOptionsName);

      preSlideInRunnable = branchNameAndPreSlideInRunnable._2();
      val branchName = branchNameAndPreSlideInRunnable._1();

      if (!slideInOptionsName.equals(branchName)) {
        val branchNameFromNewBranchDialog = branchName != null ? branchName : "no name provided";
        VcsNotifier.getInstance(project).notifyWeakError(/* displayId */ null,
            /* title */ "",
            getString("action.GitMachete.BaseSlideInBelowAction.notification.message.mismatched-names.HTML")
                .fmt(escapeHtml4(slideInOptionsName), escapeHtml4(branchNameFromNewBranchDialog)));
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

    new SlideInNonRootBackgroundable(
        gitRepository,
        branchLayout,
        branchLayoutWriter,
        getGraphTable(anActionEvent),
        preSlideInRunnable,
        slideInOptions,
        parentName).queue();
  }

  @ContinuesInBackground
  @SuppressWarnings("KotlinInternalInJava")
  private Tuple2<@Nullable String, UiThreadUnsafeRunnable> getBranchNameAndPreSlideInRunnable(
      GitRepository gitRepository,
      String startPoint,
      String initialName) {
    val repositories = java.util.Collections.singletonList(gitRepository);
    val project = gitRepository.getProject();
    val gitNewBranchDialog = new MyGitNewBranchDialog(project,
        repositories,
        /* title */ getNonHtmlString("action.GitMachete.BaseSlideInBelowAction.dialog.create-new-branch.title").fmt(startPoint),
        initialName,
        /* showCheckOutOption */ true,
        /* showResetOption */ true,
        /* showSetTrackingOption */ false);

    val options = gitNewBranchDialog.showAndGetOptions();

    if (options == null) {
      log().debug("Name of branch to slide in is null: " +
          "most likely the action has been canceled from create-new-branch dialog");
      return Tuple.of(null, () -> {});
    }

    val branchName = options.getName();
    if (!initialName.equals(branchName)) {
      return Tuple.of(branchName, () -> {});
    }

    val remoteBranch = getGitRemoteBranch(gitRepository, branchName);

    if (remoteBranch == null) {
      return Tuple.of(branchName, () -> {
        // TODO (#1938): replace with a non-reflective call once 2024.2 is no longer supported
        Constructor constructor;
        try {
          // Since 243.19420.21-EAP-SNAPSHOT
          constructor = GitBranchCheckoutOperation.class.getConstructor(Project.class, java.util.Collection.class);
        } catch (NoSuchMethodException e) {
          try {
            // Before 243.19420.21-EAP-SNAPSHOT
            constructor = GitBranchCheckoutOperation.class.getConstructor(Project.class, java.util.List.class);
          } catch (NoSuchMethodException e1) {
            throw new RuntimeException(e1);
          }
        }
        GitBranchCheckoutOperation gitBranchCheckoutOperation;
        try {
          gitBranchCheckoutOperation = (GitBranchCheckoutOperation) constructor.newInstance(project,
              Collections.singletonList(gitRepository));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
        val git4IdeaOptions = options.toGit4IdeaOptions();
        try {
          // Since 233.10527.20-EAP-SNAPSHOT
          Method perform = GitBranchCheckoutOperation.class.getDeclaredMethod("perform", String.class,
              GitNewBranchOptions.class, Runnable.class);
          try {
            // TODO (#1755): replace with a non-reflective call once 2023.2 is no longer supported
            @SuppressWarnings("nullness:argument") val ignore = perform.invoke(gitBranchCheckoutOperation, startPoint,
                git4IdeaOptions, /* callInAwtLater */ null);
          } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        } catch (NoSuchMethodException e) {
          try {
            // Before 233.10527.20-EAP-SNAPSHOT
            Method perform = GitBranchCheckoutOperation.class.getDeclaredMethod("perform", String.class,
                GitNewBranchOptions.class);
            perform.invoke(gitBranchCheckoutOperation, startPoint, git4IdeaOptions);
          } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
            throw new RuntimeException(e1);
          }
        }
      });

    } else if (options.shouldCheckout()) {
      return Tuple.of(branchName, new UiThreadUnsafeRunnable() {
        @UIThreadUnsafe
        @SuppressWarnings("removal")
        @Override
        public void run() {
          RemoteBranchActions.CheckoutRemoteBranchAction.checkoutRemoteBranch(project, repositories, remoteBranch.getName());
        }
      });

    } else {
      val refspec = createRefspec("refs/remotes/" + remoteBranch.getName(),
          "refs/heads/" + branchName, /* allowNonFastForward */ false);
      return Tuple.of(branchName, () -> new FetchBackgroundable(
          gitRepository,
          LOCAL_REPOSITORY_NAME,
          refspec,
          "Fetching Remote Branch",
          getNonHtmlString("action.GitMachete.Pull.notification.title.pull-fail").fmt(branchName),
          getString("action.GitMachete.Pull.notification.title.pull-success.HTML").fmt(branchName)).queue());
    }
  }

  @Value
  static class RemoteAndBranch {
    GitRemote remote;
    GitRemoteBranch branch;
  }

  private static @Nullable GitRemoteBranch getGitRemoteBranch(GitRepository gitRepository, String branchName) {

    val remotesWithBranch = List.ofAll(gitRepository.getRemotes())
        .<RemoteAndBranch>flatMap(r -> {
          val remoteBranchName = r.getName() + "/" + branchName;
          GitRemoteBranch remoteBranch = gitRepository.getBranches().findRemoteBranch(remoteBranchName);
          return remoteBranch != null ? Option.some(new RemoteAndBranch(r, remoteBranch)) : Option.none();
        })
        // Note: false < true. Hence, the pair with origin will be first (head) if exists.
        .sortBy(t -> !t.remote.getName().equals("origin"));

    if (remotesWithBranch.isEmpty()) {
      return null;
    }

    val chosen = remotesWithBranch.head();
    if (remotesWithBranch.size() > 1) {
      val title = getString("action.GitMachete.BaseSlideInBelowAction.notification.title.multiple-remotes");
      val message = getString("action.GitMachete.BaseSlideInBelowAction.notification.message.multiple-remotes")
          .fmt(chosen.branch.getName(), chosen.remote.getName());
      VcsNotifier.getInstance(gitRepository.getProject()).notifyInfo(/* displayId */ null, title, message);
    }
    return chosen.branch;
  }
}
