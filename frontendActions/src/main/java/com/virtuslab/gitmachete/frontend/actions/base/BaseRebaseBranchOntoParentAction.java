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
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actionids.ActionPlaces;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedVcsRepository;

@CustomLog
public abstract class BaseRebaseBranchOntoParentAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyProject,
      IExpectsKeySelectedVcsRepository {

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    Option<Repository.State> state = getSelectedVcsRepository(anActionEvent).map(r -> r.getState());

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
    LOG.debug("Performing");

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));
    if (branch.isDefined() && branch.get().isNonRootBranch()) {
      doRebase(anActionEvent, branch.get().asNonRootBranch());
    } else {
      LOG.warn("Skipping the action because branch is undefined or is a root branch: branch='${branch}'");
    }
  }

  private void doRebase(AnActionEvent anActionEvent, IGitMacheteNonRootBranch branchToRebase) {
    var project = getProject(anActionEvent);
    var gitRepository = getSelectedVcsRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      doRebase(project, gitRepository.get(), branchToRebase);
    } else {
      LOG.warn("Skipping the action because no VCS repository is selected");
    }
  }

  private void doRebase(Project project, GitRepository gitRepository, IGitMacheteNonRootBranch branchToRebase) {
    LOG.debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}, branchToRebase = ${branchToRebase}");

    Try.of(() -> branchToRebase.getParametersForRebaseOntoParent())
        .onSuccess(gitRebaseParameters -> {
          LOG.debug(() -> "Queuing '${branchToRebase.getName()}' branch rebase background task");

          new Task.Backgroundable(project, "Rebasing") {
            @Override
            public void run(ProgressIndicator indicator) {
              GitRebaseParams params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParameters);
              LOG.info("Rebasing '${gitRebaseParameters.getCurrentBranch().getName()}' branch " +
                  "until ${gitRebaseParameters.getForkPointCommit().getHash()} commit " +
                  "onto ${gitRebaseParameters.getNewBaseCommit().getHash()}");

              GitRebaseUtils.rebase(project, java.util.List.of(gitRepository), params, indicator);
            }

            // TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential
            // changes to commits (eg. commits may get squashed so the graph structure changes).
          }.queue();
        }).onFailure(e -> {
          // TODO (#172): redirect the user to the manual fork-point
          var message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
          LOG.error(message);
          VcsNotifier.getInstance(project).notifyError("Rebase failed", message);
        });
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
