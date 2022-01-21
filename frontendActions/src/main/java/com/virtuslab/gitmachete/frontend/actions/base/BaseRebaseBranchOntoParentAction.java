package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseEditorHandler;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import git4idea.util.GitFreezingProcess;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedBranchAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.compat.IntelliJNotificationCompat;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public abstract class BaseRebaseBranchOntoParentAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {
  private static final String NL = System.lineSeparator();

  @Override
  public IEnhancedLambdaLogger log() {
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

    val state = getSelectedGitRepository(anActionEvent).map(r -> r.getState());
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU);

    if (state.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.disabled.repository.unknown-state"));

    } else if (state.get() != Repository.State.NORMAL
        && !(isCalledFromContextMenu && state.get() == Repository.State.DETACHED)) {

      val stateName = Match(state.get()).of(
          Case($(Repository.State.GRAFTING),
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.repository.state.ongoing.cherry-pick")),
          Case($(Repository.State.DETACHED),
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.repository.state.detached-head")),
          Case($(Repository.State.MERGING),
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.repository.state.ongoing.merge")),
          Case($(Repository.State.REBASING),
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.repository.state.ongoing.rebase")),
          Case($(Repository.State.REVERTING),
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.repository.state.ongoing.revert")),
          Case($(), ": " + state.get().name().toLowerCase()));

      presentation.setEnabled(false);
      presentation.setDescription(format(
          getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.disabled.repository.status"), stateName));
    } else {

      val branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
      val branch = branchName != null
          ? getManagedBranchByName(anActionEvent, branchName).getOrNull()
          : null;

      if (branch == null) {
        presentation.setEnabled(false);
        presentation.setDescription(format(getString("action.GitMachete.description.disabled.undefined.machete-branch"),
            "Rebase", getQuotedStringOrCurrent(branchName)));
      } else if (branch.isRoot()) {

        if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
          presentation.setEnabled(false);
          presentation.setDescription(
              format(getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.disabled.root-branch"),
                  branch.getName()));
        } else { //contextmenu
          // in case of root branch we do not want to show this option at all
          presentation.setEnabledAndVisible(false);
        }

      } else if (branch.isNonRoot()) {
        val nonRootBranch = branch.asNonRoot();
        IManagedBranchSnapshot upstream = nonRootBranch.getParent();
        presentation.setDescription(format(getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description"),
            branch.getName(), upstream.getName()));
      }

      val isRebasingCurrent = branch != null && getCurrentBranchNameIfManaged(anActionEvent)
          .map(bn -> bn.equals(branch.getName())).getOrElse(false);
      if (isCalledFromContextMenu && isRebasingCurrent) {
        presentation.setText(getString("action.GitMachete.BaseRebaseBranchOntoParentAction.text"));
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName.flatMap(bn -> getManagedBranchByName(anActionEvent, bn));

    if (branch.isDefined()) {
      if (branch.get().isNonRoot()) {
        doRebase(anActionEvent, branch.get().asNonRoot());
      } else {
        LOG.warn("Skipping the action because the branch '${branch.get().getName()}' is a root branch");
      }
    }
  }

  private void doRebase(AnActionEvent anActionEvent, INonRootManagedBranchSnapshot branchToRebase) {
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    val state = gitRepository.map(r -> r.getState());
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU);
    val shouldExplicitlyCheckout = isCalledFromContextMenu && state.map(s -> Repository.State.DETACHED == s).getOrElse(false);

    if (gitRepository.isDefined() && gitMacheteRepositorySnapshot.isDefined()) {
      doRebase(project, gitRepository.get(), gitMacheteRepositorySnapshot.get(), branchToRebase, shouldExplicitlyCheckout);
    }
  }

  private void doRebase(
      Project project,
      GitRepository gitRepository,
      IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot,
      INonRootManagedBranchSnapshot branchToRebase,
      boolean shouldExplicitlyCheckout) {
    LOG.debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}, branchToRebase = ${branchToRebase}");

    val tryGitRebaseParameters = Try.of(() -> branchToRebase.getParametersForRebaseOntoParent());

    if (tryGitRebaseParameters.isFailure()) {
      val e = tryGitRebaseParameters.getCause();
      // TODO (#172): redirect the user to the manual fork-point
      val message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
      LOG.error(message);
      IntelliJNotificationCompat.notifyError(project,
          getString("action.GitMachete.BaseRebaseBranchOntoParentAction.notification.title.rebase-fail"), message);
      return;
    }

    val gitRebaseParameters = tryGitRebaseParameters.get();
    LOG.debug(() -> "Queuing machete-pre-rebase hooks background task for '${branchToRebase.getName()}' branch");

    new Task.Backgroundable(project, getString("action.GitMachete.BaseRebaseBranchOntoParentAction.hook.task-title")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {

        AtomicReference<Try<Option<IExecutionResult>>> wrapper = new AtomicReference<>(Try.success(Option.none()));
        new GitFreezingProcess(project, myTitle, () -> {
          LOG.info("Executing machete-pre-rebase hooks");
          val hookResult = Try
              .of(() -> gitMacheteRepositorySnapshot.executeMachetePreRebaseHookIfPresent(gitRebaseParameters));
          wrapper.set(hookResult);
        }).execute();
        Try<Option<IExecutionResult>> hookResult = wrapper.get();
        if (hookResult == null) {
          // Not really possible, it's here just to calm down Checker Framework.
          return;
        }

        if (hookResult.isFailure()) {
          val message = "machete-pre-rebase hooks refused to rebase ${NL}error: ${hookResult.getCause().getMessage()}";
          LOG.error(message);
          IntelliJNotificationCompat.notifyError(project,
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.notification.title.rebase-abort"),
              message);
          return;
        }

        val maybeExecutionResult = hookResult.get();
        if (maybeExecutionResult.isDefined() && maybeExecutionResult.get().getExitCode() != 0) {
          val message = "machete-pre-rebase hooks refused to rebase (exit code ${maybeExecutionResult.get().getExitCode()})";
          LOG.error(message);
          val executionResult = maybeExecutionResult.get();
          val stdoutOption = executionResult.getStdout();
          val stderrOption = executionResult.getStderr();
          IntelliJNotificationCompat.notifyError(project,
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.notification.title.rebase-abort"), message
                  + (!stdoutOption.trim().isEmpty() ? NL + "stdout:" + NL + stdoutOption : "")
                  + (!stderrOption.trim().isEmpty() ? NL + "stderr:" + NL + stderrOption : ""));
          return;
        }

        LOG.debug(() -> "Queuing rebase background task for '${branchToRebase.getName()}' branch");

        new Task.Backgroundable(project, getString("action.GitMachete.BaseRebaseBranchOntoParentAction.task-title")) {
          @Override
          @UIThreadUnsafe
          public void run(ProgressIndicator indicator) {
            GitRebaseParams params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParameters);
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
              CheckoutSelectedBranchAction.doCheckout(
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
    GitVersion gitVersion = repository.getVcs().getVersion();
    String currentBranchName = gitRebaseParameters.getCurrentBranch().getName();
    String newBaseBranchFullName = gitRebaseParameters.getNewBaseBranch().getFullName();
    String forkPointCommitHash = gitRebaseParameters.getForkPointCommit().getHash();

    // TODO (#743): replace with a non-reflective constructor call
    try {
      // Proper solution for 2020.3+
      val constructor = GitRebaseParams.class.getConstructor(GitVersion.class, String.class, String.class, String.class,
          java.util.Set.class, GitRebaseParams.AutoSquashOption.class, GitRebaseEditorHandler.class);

      val gitRebaseOptionValueOf = Class.forName("git4idea.rebase.GitRebaseOption").getMethod("valueOf", String.class);
      val INTERACTIVE = gitRebaseOptionValueOf.invoke(null, "INTERACTIVE");
      val KEEP_EMPTY = gitRebaseOptionValueOf.invoke(null, "KEEP_EMPTY");

      val options = kotlin.collections.SetsKt.hashSetOf(INTERACTIVE, KEEP_EMPTY);
      return constructor.newInstance(gitVersion, currentBranchName, newBaseBranchFullName,
          /* upstream */ forkPointCommitHash, /* selectedOptions */ options, GitRebaseParams.AutoSquashOption.DEFAULT,
          /* editorHandler */ null);
    } catch (ReflectiveOperationException e) {
      // Fallback for 2020.1 and 2020.2
      return new GitRebaseParams(gitVersion, currentBranchName, newBaseBranchFullName, /* upstream */ forkPointCommitHash,
          /* interactive */ true, /* preserveMerges */ false);
    }
  }
}
