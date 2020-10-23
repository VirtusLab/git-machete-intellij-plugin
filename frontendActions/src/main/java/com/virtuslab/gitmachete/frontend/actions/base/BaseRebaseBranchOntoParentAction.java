package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import git4idea.util.GitFreezingProcess;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedBranchAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;

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

    var presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var state = getSelectedGitRepository(anActionEvent).map(r -> r.getState());
    var isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU);

    if (state.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description.disabled.repository.unknown-state"));

    } else if (state.get() != Repository.State.NORMAL
        && !(isCalledFromContextMenu && state.get() == Repository.State.DETACHED)) {

      var stateName = Match(state.get()).of(
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

      var branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
      var branch = branchName != null
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
        var nonRootBranch = branch.asNonRoot();
        IManagedBranchSnapshot upstream = nonRootBranch.getParent();
        presentation.setDescription(format(getString("action.GitMachete.BaseRebaseBranchOntoParentAction.description"),
            branch.getName(), upstream.getName()));
      }

      var isRebasingCurrent = branch != null && getCurrentBranchNameIfManaged(anActionEvent)
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

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getManagedBranchByName(anActionEvent, bn));

    if (branch.isDefined()) {
      if (branch.get().isNonRoot()) {
        doRebase(anActionEvent, branch.get().asNonRoot());
      } else {
        LOG.warn("Skipping the action because the branch '${branch.get().getName()}' is a root branch");
      }
    }
  }

  private void doRebase(AnActionEvent anActionEvent, INonRootManagedBranchSnapshot branchToRebase) {
    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    var state = gitRepository.map(r -> r.getState());
    var isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU);
    var shouldExplicitlyCheckout = isCalledFromContextMenu && state.map(s -> Repository.State.DETACHED == s).getOrElse(false);

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

    var tryGitRebaseParameters = Try.of(() -> branchToRebase.getParametersForRebaseOntoParent());

    if (tryGitRebaseParameters.isFailure()) {
      var e = tryGitRebaseParameters.getCause();
      // TODO (#172): redirect the user to the manual fork-point
      var message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
      LOG.error(message);
      VcsNotifier.getInstance(project)
          .notifyError(getString("action.GitMachete.BaseRebaseBranchOntoParentAction.notification.title.rebase-fail"), message);
      return;
    }

    var gitRebaseParameters = tryGitRebaseParameters.get();
    LOG.debug(() -> "Queuing machete-pre-rebase hooks background task for '${branchToRebase.getName()}' branch");

    new Task.Backgroundable(project, getString("action.GitMachete.BaseRebaseBranchOntoParentAction.hook.task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {

        var wrapper = new Object() {
          Try<Option<IExecutionResult>> hookResult = Try.success(Option.none());
        };
        new GitFreezingProcess(project, myTitle, () -> {
          LOG.info("Executing machete-pre-rebase hooks");
          wrapper.hookResult = Try
              .of(() -> gitMacheteRepositorySnapshot.executeMachetePreRebaseHookIfPresent(gitRebaseParameters));
        }).execute();
        var hookResult = wrapper.hookResult;

        if (hookResult.isFailure()) {
          var message = "machete-pre-rebase hooks refused to rebase ${NL}error: ${hookResult.getCause().getMessage()}";
          LOG.error(message);
          VcsNotifier.getInstance(project)
              .notifyError(getString("action.GitMachete.BaseRebaseBranchOntoParentAction.notification.title.rebase-abort"),
                  message);
          return;
        }

        var maybeExecutionResult = hookResult.get();
        if (maybeExecutionResult.isDefined() && maybeExecutionResult.get().getExitCode() != 0) {
          var message = "machete-pre-rebase hooks refused to rebase (exit code ${maybeExecutionResult.get().getExitCode()})";
          LOG.error(message);
          var executionResult = maybeExecutionResult.get();
          var stdoutOption = executionResult.getStdout();
          var stderrOption = executionResult.getStderr();
          VcsNotifier.getInstance(project).notifyError(
              getString("action.GitMachete.BaseRebaseBranchOntoParentAction.notification.title.rebase-abort"), message
                  + (!stdoutOption.isBlank() ? NL + "stdout:" + NL + stdoutOption : "")
                  + (!stderrOption.isBlank() ? NL + "stderr:" + NL + stderrOption : ""));
          return;
        }

        LOG.debug(() -> "Queuing rebase background task for '${branchToRebase.getName()}' branch");

        new Task.Backgroundable(project, getString("action.GitMachete.BaseRebaseBranchOntoParentAction.task-title")) {
          @Override
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
            GitRebaseUtils.rebase(project, java.util.List.of(gitRepository), params, indicator);
          }
        }.queue();
      }

      // TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential
      // changes to commits (eg. commits may get squashed so the graph structure changes).
    }.queue();
  }

  private GitRebaseParams getIdeaRebaseParamsOf(GitRepository repository, IGitRebaseParameters gitRebaseParameters) {
    GitVersion gitVersion = repository.getVcs().getVersion();
    String currentBranchName = gitRebaseParameters.getCurrentBranch().getName();
    String newBaseBranchFullName = gitRebaseParameters.getNewBaseBranch().getFullName();
    String forkPointCommitHash = gitRebaseParameters.getForkPointCommit().getHash();

    return new GitRebaseParams(gitVersion, currentBranchName, newBaseBranchFullName, /* upstream */ forkPointCommitHash,
        /* interactive */ true, /* preserveMerges */ false);
  }
}
