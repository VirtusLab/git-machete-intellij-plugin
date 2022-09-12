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
import com.intellij.openapi.actionSystem.Presentation;
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

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
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

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    final var selectedGitRepo = getSelectedGitRepository(anActionEvent);
    final var state = selectedGitRepo != null ? selectedGitRepo.getState() : null;
    final var isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU);

    if (state == null) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.repository.unknown-state"));

    } else if (state != Repository.State.NORMAL
        && !(isCalledFromContextMenu && state == Repository.State.DETACHED)) {

      final var stateName = Match(state).of(
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

      final var branchName = getNameOfBranchUnderAction(anActionEvent);
      final var branch = getManagedBranchByName(anActionEvent, branchName);

      if (branch == null) {
        presentation.setEnabled(false);
        presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.undefined.machete-branch")
            .format("Rebase", getQuotedStringOrCurrent(branchName)));
      } else if (branch.isRoot()) {

        if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
          presentation.setEnabled(false);
          presentation.setDescription(
              getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.root-branch")
                  .format(branch.getName()));
        } else { //contextmenu
          // in case of root branch we do not want to show this option at all
          presentation.setEnabledAndVisible(false);
        }

      } else if (branch.isNonRoot()) {
        final var nonRootBranch = branch.asNonRoot();
        final var upstream = nonRootBranch.getParent();
        presentation.setDescription(getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description")
            .format(branch.getName(), upstream.getName()));
      }

      final var currentBranchNameIfManaged = getCurrentBranchNameIfManaged(anActionEvent);

      final var isRebasingCurrent = branch != null && currentBranchNameIfManaged != null
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

    final var branchName = getNameOfBranchUnderAction(anActionEvent);
    final var branch = getManagedBranchByName(anActionEvent, branchName);

    if (branch != null) {
      if (branch.isNonRoot()) {
        doRebase(anActionEvent, branch.asNonRoot());
      } else {
        LOG.warn("Skipping the action because the branch '${branch.getName()}' is a root branch");
      }
    }
  }

  private void doRebase(AnActionEvent anActionEvent, INonRootManagedBranchSnapshot branchToRebase) {
    final var project = getProject(anActionEvent);
    final var gitRepository = getSelectedGitRepository(anActionEvent);
    final var gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    final var state = gitRepository != null ? gitRepository.getState() : null;
    final var isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU);
    final var shouldExplicitlyCheckout = isCalledFromContextMenu
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

    final var tryGitRebaseParameters = Try.of(branchToRebase::getParametersForRebaseOntoParent);

    if (tryGitRebaseParameters.isFailure()) {
      final var e = tryGitRebaseParameters.getCause();
      // TODO (#172): redirect the user to the manual fork-point
      final var message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
      LOG.error(message);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-fail"), message);
      return;
    }

    IGitRebaseParameters gitRebaseParameters = tryGitRebaseParameters.get();
    LOG.debug(() -> "Queuing machete-pre-rebase hooks background task for '${branchToRebase.getName()}' branch");

    new Task.Backgroundable(project, getString("action.GitMachete.BaseSyncToParentByRebaseAction.hook.task-title")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {

        final AtomicReference<Try<Option<IExecutionResult>>> wrapper = new AtomicReference<>(Try.success(Option.none()));
        new GitFreezingProcess(project, getTitle(), () -> {
          LOG.info("Executing machete-pre-rebase hooks");
          final var hookResult = Try
              .of(() -> Option.of(gitMacheteRepositorySnapshot.executeMachetePreRebaseHookIfPresent(gitRebaseParameters)));
          wrapper.set(hookResult);
        }).execute();
        final var hookResult = wrapper.get();
        if (hookResult == null) {
          // Not really possible, it's here just to calm down Checker Framework.
          return;
        }

        if (hookResult.isFailure()) {
          final var message = "machete-pre-rebase hooks refused to rebase ${NL}error: ${hookResult.getCause().getMessage()}";
          LOG.error(message);
          VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"),
              message);
          return;
        }

        final var maybeExecutionResult = hookResult.get();
        if (maybeExecutionResult.isDefined() && maybeExecutionResult.get().getExitCode() != 0) {
          final var message = "machete-pre-rebase hooks refused to rebase (exit code ${maybeExecutionResult.get().getExitCode()})";
          LOG.error(message);
          final var executionResult = maybeExecutionResult.get();
          final var stdoutOption = executionResult.getStdout();
          final var stderrOption = executionResult.getStderr();
          VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
              getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"), message
                  + (!stdoutOption.trim().isEmpty() ? NL + "stdout:" + NL + stdoutOption : "")
                  + (!stderrOption.trim().isEmpty() ? NL + "stderr:" + NL + stderrOption : ""));
          return;
        }

        LOG.debug(() -> "Queuing rebase background task for '${branchToRebase.getName()}' branch");

        new Task.Backgroundable(project, getString("action.GitMachete.BaseSyncToParentByRebaseAction.task-title")) {
          @Override
          @UIThreadUnsafe
          public void run(ProgressIndicator indicator) {
            final var params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParameters);
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
    final var gitVersion = repository.getVcs().getVersion();
    final var currentBranchName = gitRebaseParameters.getCurrentBranch().getName();
    final var newBaseBranchFullName = gitRebaseParameters.getNewBaseBranch().getFullName();
    final var forkPointCommitHash = gitRebaseParameters.getForkPointCommit().getHash();

    // TODO (#1114): remove the mechanism for checking the availability of "--empty=drop"
    val options = kotlin.collections.SetsKt.hashSetOf(GitRebaseOption.INTERACTIVE);

    isGitRebaseOptionEntryAvailable("--empty=drop", gitVersion).forEach(options::add);

    return new GitRebaseParams(gitVersion, currentBranchName, newBaseBranchFullName,
        /* upstream */ forkPointCommitHash, /* selectedOptions */ options, GitRebaseParams.AutoSquashOption.DEFAULT,
        /* editorHandler */ null);
  }

  @UIThreadUnsafe
  private Option<GitRebaseOption> isGitRebaseOptionEntryAvailable(String optionText, GitVersion gitVersion) {
    val maybeEmptyDropEntry = Arrays.stream(GitRebaseOption.values())
        .filter(entry -> entry.getOption(gitVersion).equals(optionText))
        .findFirst();

    return Option.ofOptional(maybeEmptyDropEntry);
  }
}
