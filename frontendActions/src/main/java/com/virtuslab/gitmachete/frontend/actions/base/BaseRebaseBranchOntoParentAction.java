package com.virtuslab.gitmachete.frontend.actions.base;

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

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseRebaseBranchOntoParentAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyProject,
      IExpectsKeyGitMacheteRepository {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var state = getSelectedGitRepository(anActionEvent).map(r -> r.getState());

    if (state.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Can't rebase due to unknown repository state");

    } else if (state.get() != Repository.State.NORMAL) {

      var stateName = Match(state.get()).of(
          Case($(Repository.State.GRAFTING), "during an ongoing cherry-pick"),
          Case($(Repository.State.DETACHED), "in the detached head state"),
          Case($(Repository.State.MERGING), "during an ongoing merge"),
          Case($(Repository.State.REBASING), "during an ongoing rebase"),
          Case($(Repository.State.REVERTING), "during an ongoing revert"),
          Case($(), ": " + state.toString().toLowerCase()));

      presentation.setEnabled(false);
      presentation.setDescription("Can't rebase ${stateName}");
    } else {

      var branchName = getNameOfBranchUnderAction(anActionEvent);
      var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));

      if (branch.isEmpty()) {
        presentation.setEnabled(false);
        presentation.setDescription("Rebase disabled due to undefined selected branch");
        return;
      }

      assert branchName.isDefined() : "branchName is undefined";
      if (branch.get().isRootBranch()) {

        if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
          presentation.setEnabled(false);
          presentation.setDescription("Root branch '${branchName.get()}' cannot be rebased");
        } else { //contextmenu
          // in case of root branch we do not want to show this option at all
          presentation.setEnabledAndVisible(false);
        }

      } else if (branch.get().asNonRootBranch().getSyncToParentStatus().equals(SyncToParentStatus.MergedToParent)) {
        presentation.setEnabled(false);
        presentation.setDescription("Can't rebase merged branch '${branchName.get()}'");

      } else if (branch.get().isNonRootBranch()) {
        var nonRootBranch = branch.get().asNonRootBranch();
        IGitMacheteBranch upstream = nonRootBranch.getUpstreamBranch();
        presentation.setDescription("Rebase '${branchName}' onto '${upstream.getName()}'");
      }
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));
    if (branch.isDefined()) {
      if (branch.get().isNonRootBranch()) {
        doRebase(anActionEvent, branch.get().asNonRootBranch());
      } else {
        log().warn("Skipping the action because branch is a root branch: branch='${branch}'");
      }
    }
  }

  private void doRebase(AnActionEvent anActionEvent, IGitMacheteNonRootBranch branchToRebase) {
    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var gitMacheteRepository = getGitMacheteRepositoryWithLoggingOnEmpty(anActionEvent);

    if (gitRepository.isDefined() && gitMacheteRepository.isDefined()) {
      doRebase(project, gitRepository.get(), gitMacheteRepository.get(), branchToRebase);
    }
  }

  private void doRebase(
      Project project,
      GitRepository gitRepository,
      IGitMacheteRepository gitMacheteRepository,
      IGitMacheteNonRootBranch branchToRebase) {
    log().debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}, branchToRebase = ${branchToRebase}");

    var tryGitRebaseParameters = Try.of(() -> branchToRebase.getParametersForRebaseOntoParent());

    if (tryGitRebaseParameters.isFailure()) {
      var e = tryGitRebaseParameters.getCause();
      // TODO (#172): redirect the user to the manual fork-point
      var message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
      log().error(message);
      VcsNotifier.getInstance(project).notifyError("Rebase failed", message);
      return;
    }

    var gitRebaseParameters = tryGitRebaseParameters.get();
    log().debug(() -> "Queuing machete-pre-rebase hook background task for '${branchToRebase.getName()}' branch");

    new Task.Backgroundable(project, "Running machete-pre-rebase hook") {
      @Override
      public void run(ProgressIndicator indicator) {

        var wrapper = new Object() {
          Try<Option<Integer>> hookResult = Try.success(Option.none());
        };
        new GitFreezingProcess(project, myTitle, () -> {
          log().info("Executing machete-pre-rebase hook");
          wrapper.hookResult = Try
              .of(() -> gitMacheteRepository.executeMachetePreRebaseHookIfPresent(gitRebaseParameters));
        }).execute();
        var hookResult = wrapper.hookResult;

        if (hookResult.isFailure()) {
          var message = "machete-pre-rebase hook refused to rebase (error: ${hookResult.getCause().getMessage()})";
          log().error(message);
          VcsNotifier.getInstance(project).notifyError("Rebase aborted", message);
          return;
        }

        var maybeExitCode = hookResult.get();
        if (maybeExitCode.isDefined() && maybeExitCode.get() != 0) {
          var message = "machete-pre-rebase hook refused to rebase (exit code ${maybeExitCode.get()})";
          log().error(message);
          VcsNotifier.getInstance(project).notifyError("Rebase aborted", message);
          return;
        }

        log().debug(() -> "Queuing rebase background task for '${branchToRebase.getName()}' branch");

        new Task.Backgroundable(project, "Rebasing") {
          @Override
          public void run(ProgressIndicator indicator) {
            GitRebaseParams params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParameters);
            log().info("Rebasing '${gitRebaseParameters.getCurrentBranch().getName()}' branch " +
                "until ${gitRebaseParameters.getForkPointCommit().getHash()} commit " +
                "onto ${gitRebaseParameters.getNewBaseCommit().getHash()}");

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
    String currentBranch = gitRebaseParameters.getCurrentBranch().getName();
    String newBase = gitRebaseParameters.getNewBaseCommit().getHash();
    String forkPoint = gitRebaseParameters.getForkPointCommit().getHash();

    return new GitRebaseParams(gitVersion, currentBranch, newBase, /* upstream */ forkPoint,
        /* interactive */ true, /* preserveMerges */ false);
  }
}
