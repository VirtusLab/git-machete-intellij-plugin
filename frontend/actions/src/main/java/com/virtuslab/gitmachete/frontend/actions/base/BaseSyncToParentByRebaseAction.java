package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseOption;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import git4idea.util.GitFreezingProcess;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({Arrays.class, GitMacheteBundle.class})
@CustomLog
public abstract class BaseSyncToParentByRebaseAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {
  private static final String NL = System.lineSeparator();

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

    val selectedGitRepo = getSelectedGitRepository(anActionEvent);
    val state = selectedGitRepo != null ? selectedGitRepo.getState() : null;
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU);

    if (state == null) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.repository.unknown-state"));

    } else if (state != Repository.State.NORMAL
        && !(isCalledFromContextMenu && state == Repository.State.DETACHED)) {

      val stateName = Match(state).of(
          Case($(Repository.State.GRAFTING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.cherry-pick")),
          Case($(Repository.State.DETACHED),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.detached-head")),
          Case($(Repository.State.MERGING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.merge")),
          Case($(Repository.State.REBASING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.rebase")),
          Case($(Repository.State.REVERTING),
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.description.repository.state.ongoing.revert")),
          Case($(), ": " + state.name().toLowerCase()));

      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.repository.status")
              .format(stateName));
    } else {

      val branchName = getNameOfBranchUnderAction(anActionEvent);
      val branch = getManagedBranchByName(anActionEvent, branchName);

      if (branch == null) {
        presentation.setEnabled(false);
        presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.undefined.machete-branch")
            .format("Rebase", getQuotedStringOrCurrent(branchName)));
      } else if (branch.isRoot()) {

        if (anActionEvent.getPlace().equals(ActionPlaces.TOOLBAR)) {
          presentation.setEnabled(false);
          presentation.setDescription(
              getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.root-branch")
                  .format(branch.getName()));
        } else { //contextmenu
          // in case of root branch we do not want to show this option at all
          presentation.setEnabledAndVisible(false);
        }

      } else if (branch.isNonRoot()) {
        val nonRootBranch = branch.asNonRoot();
        val upstream = nonRootBranch.getParent();
        presentation.setDescription(getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description")
            .format(branch.getName(), upstream.getName()));
      }

      val currentBranchNameIfManaged = getCurrentBranchNameIfManaged(anActionEvent);

      val isRebasingCurrent = branch != null && currentBranchNameIfManaged != null
          && currentBranchNameIfManaged.equals(branch.getName());
      if (isCalledFromContextMenu && isRebasingCurrent) {
        presentation.setText(getString("action.GitMachete.BaseSyncToParentByRebaseAction.text"));
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchName);

    if (branch != null) {
      if (branch.isNonRoot()) {
        doRebase(anActionEvent, branch.asNonRoot());
      } else {
        LOG.warn("Skipping the action because the branch '${branch.getName()}' is a root branch");
      }
    }
  }

  private void doRebase(AnActionEvent anActionEvent, INonRootManagedBranchSnapshot branchToRebase) {
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    val state = gitRepository != null ? gitRepository.getState() : null;
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU);
    val shouldExplicitlyCheckout = isCalledFromContextMenu
        && state != null && Repository.State.DETACHED == state;

    if (gitRepository != null && gitMacheteRepositorySnapshot != null) {
      doRebase(project, gitRepository, gitMacheteRepositorySnapshot, branchToRebase, shouldExplicitlyCheckout);
    }
  }

  private void doRebase(
      Project project,
      GitRepository gitRepository,
      IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot,
      INonRootManagedBranchSnapshot branchToRebase,
      boolean shouldExplicitlyCheckout) {
    LOG.debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}, branchToRebase = ${branchToRebase}");

    val tryGitRebaseParameters = Try.of(branchToRebase::getParametersForRebaseOntoParent);

    if (tryGitRebaseParameters.isFailure()) {
      val e = tryGitRebaseParameters.getCause();
      val message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
      LOG.error(message);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-fail"), message);
      return;
    }

    val gitRebaseParameters = tryGitRebaseParameters.get();
    LOG.debug(() -> "Queuing machete-pre-rebase hooks background task for '${branchToRebase.getName()}' branch");

    new Task.Backgroundable(project, getString("action.GitMachete.BaseSyncToParentByRebaseAction.hook.task-title")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {

        final AtomicReference<Try<@Nullable IExecutionResult>> wrapper = new AtomicReference<>(Try.success(null));
        new GitFreezingProcess(project, getTitle(), () -> {
          LOG.info("Executing machete-pre-rebase hooks");
          Try<@Nullable IExecutionResult> hookResult = Try
              .of(() -> gitMacheteRepositorySnapshot.executeMachetePreRebaseHookIfPresent(gitRebaseParameters));
          wrapper.set(hookResult);
        }).execute();
        Try<@Nullable IExecutionResult> hookResult = wrapper.get();
        if (hookResult == null) {
          // Not really possible, it's here just to calm down Checker Framework.
          return;
        }

        if (hookResult.isFailure()) {
          val message = "machete-pre-rebase hooks refused to rebase ${NL}error: ${hookResult.getCause().getMessage()}";
          LOG.error(message);
          VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"),
              message);
          return;
        }

        val maybeExecutionResult = hookResult.get();
        if (maybeExecutionResult != null && maybeExecutionResult.getExitCode() != 0) {
          val message = "machete-pre-rebase hooks refused to rebase (exit code ${maybeExecutionResult.getExitCode()})";
          LOG.error(message);
          val executionResult = maybeExecutionResult;
          val stdoutOption = executionResult.getStdout();
          val stderrOption = executionResult.getStderr();
          VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"), message
                  + (!stdoutOption.isBlank() ? NL + "stdout:" + NL + stdoutOption : "")
                  + (!stderrOption.isBlank() ? NL + "stderr:" + NL + stderrOption : ""));
          return;
        }

        LOG.debug(() -> "Queuing rebase background task for '${branchToRebase.getName()}' branch");

        new Task.Backgroundable(project, getString("action.GitMachete.BaseSyncToParentByRebaseAction.task-title")) {
          @Override
          @UIThreadUnsafe
          public void run(ProgressIndicator indicator) {
            val params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParameters);
            LOG.info("Rebasing '${gitRebaseParameters.getCurrentBranch().getName()}' branch " +
                "until ${gitRebaseParameters.getForkPointCommit().getHash()} commit " +
                "onto ${gitRebaseParameters.getNewBaseBranch().getName()}");

            /*
             * Git4Idea ({@link git4idea.rebase.GitRebaseUtils#rebase}) does not allow to rebase in detached head state.
             * However, it is possible with Git (performing checkout implicitly) and should be allowed in the case of
             * "Checkout and Rebase Onto Parent" Action. To pass the git4idea check in such a case we checkout the branch
             * explicitly and then perform the actual rebase.
             */
            if (shouldExplicitlyCheckout) {
              CheckoutSelectedAction.doCheckout(
                  project, indicator, gitRebaseParameters.getCurrentBranch().getName(), gitRepository);
            }
            GitRebaseUtils.rebase(project, Collections.singletonList(gitRepository), params, indicator);
          }
        }.queue();
      }

      // TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential
      // changes to commits (eg. commits may get squashed so the graph structure changes).
    }.queue();
  }

  @UIThreadUnsafe
  private GitRebaseParams getIdeaRebaseParamsOf(GitRepository repository, IGitRebaseParameters gitRebaseParameters) {
    val gitVersion = repository.getVcs().getVersion();
    val currentBranchName = gitRebaseParameters.getCurrentBranch().getName();
    val newBaseBranchFullName = gitRebaseParameters.getNewBaseBranch().getFullName();
    val forkPointCommitHash = gitRebaseParameters.getForkPointCommit().getHash();

    // TODO (#1114): remove the mechanism for checking the availability of "--empty=drop"
    val options = kotlin.collections.SetsKt.hashSetOf(GitRebaseOption.INTERACTIVE);

    val gitRebaseOption = getAvailableGitRebaseOptions("--empty=drop", gitVersion);
    if (gitRebaseOption != null) {
      options.add(gitRebaseOption);
    }

    return new GitRebaseParams(gitVersion, currentBranchName, newBaseBranchFullName,
        /* upstream */ forkPointCommitHash, /* selectedOptions */ options, GitRebaseParams.AutoSquashOption.DEFAULT,
        /* editorHandler */ null);
  }

  @UIThreadUnsafe
  private @Nullable GitRebaseOption getAvailableGitRebaseOptions(String optionText, GitVersion gitVersion) {
    val maybeEmptyDropEntry = GitRebaseOption.values().stream()
        .filter(entry -> entry.getOption(gitVersion).equals(optionText))
        .findFirst();

    return Option.ofOptional(maybeEmptyDropEntry).getOrNull();
  }
}
