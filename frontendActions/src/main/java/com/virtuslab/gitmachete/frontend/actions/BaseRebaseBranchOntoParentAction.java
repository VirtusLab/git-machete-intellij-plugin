package com.virtuslab.gitmachete.frontend.actions;

import static io.vavr.API.$;
import static io.vavr.API.Case;

import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitUtil;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import io.vavr.control.Try;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BaseRebaseBranchOntoParentAction extends AnAction {

  public BaseRebaseBranchOntoParentAction(String text, String actionDescription, Icon icon) {
    super(text, actionDescription, icon);
  }

  public BaseRebaseBranchOntoParentAction() {}

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    anActionEvent.getPresentation().setEnabled(true);
    anActionEvent.getPresentation().setVisible(true);
    prohibitRebaseIfRepoInForbiddenState(anActionEvent);
  }

  @Override
  public abstract void actionPerformed(AnActionEvent anActionEvent);

  IGitMacheteRepository getMacheteRepository(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY);
    assert gitMacheteRepository != null : "Can't get gitMacheteRepository";

    return gitMacheteRepository;
  }

  protected void doRebase(AnActionEvent anActionEvent, BaseGitMacheteNonRootBranch branchToRebase) {
    Project project = anActionEvent.getProject();
    assert project != null;

    IGitMacheteRepository macheteRepository = getMacheteRepository(anActionEvent);

    Try.of(() -> macheteRepository.deriveParametersForRebaseOntoParent(branchToRebase))
        .onSuccess(gitRebaseParameters -> {

          GitRepository repository = getIdeaRepository(anActionEvent);

          new Task.Backgroundable(project, "Rebasing") {
            @Override
            public void run(ProgressIndicator indicator) {
              GitRebaseParams params = getIdeaRebaseParamsOf(anActionEvent, gitRebaseParameters);
              GitRebaseUtils.rebase(project, List.of(repository), params, indicator);
            }

            // TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential
            // changes
            // to commits (eg. commits may get squashed so the graph structure changes).
          }.queue();

        }).onFailure(e -> {
          var message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
          VcsNotifier.getInstance(project).notifyError("Rebase failed", message);
        });
  }

  GitRebaseParams getIdeaRebaseParamsOf(AnActionEvent anActionEvent, IGitRebaseParameters gitRebaseParameters) {
    GitRepository repository = getIdeaRepository(anActionEvent);
    GitVersion gitVersion = repository.getVcs().getVersion();
    String currentBranch = gitRebaseParameters.getCurrentBranch().getName();
    String newBase = gitRebaseParameters.getNewBaseCommit().getHash();
    String forkPoint = gitRebaseParameters.getForkPointCommit().getHash();

    return new GitRebaseParams(gitVersion, currentBranch, newBase, /* upstream */ forkPoint,
        /* interactive */ true, /* preserveMerges */ false);
  }

  GitRepository getIdeaRepository(AnActionEvent anActionEvent) {
    Project project = anActionEvent.getProject();
    assert project != null;
    // TODO (#64): handle multiple repositories
    Iterator<GitRepository> iterator = GitUtil.getRepositories(project).iterator();
    // The visibility predicate {@link GitMacheteContentProvider.GitMacheteVisibilityPredicate} performs
    // {@link com.intellij.openapi.vcs.ProjectLevelVcsManager#checkVcsIsActive(String)} which is true when the specified
    // VCS is used by at least one module in the project. Therefore it is guaranteed that while the Git Machete plugin
    // tab is visible, a git repository exists.
    assert iterator.hasNext();
    return iterator.next();
  }

  private void prohibitRebaseIfRepoInForbiddenState(AnActionEvent anActionEvent) {
    Repository.State state = getIdeaRepository(anActionEvent).getState();
    var presentation = anActionEvent.getPresentation();
    if (state != Repository.State.NORMAL && presentation.isEnabledAndVisible()) {
      var message = getProhibitedStateMessage(state);
      presentation.setEnabled(false);
      presentation.setDescription(message);
    }
  }

  private String getProhibitedStateMessage(Repository.State state) {

    var stateName = io.vavr.API.Match(state).of(
        Case($(Repository.State.GRAFTING), "during an ongoing cherry-pick"),
        Case($(Repository.State.DETACHED), "in the detached head state"),
        Case($(Repository.State.MERGING), "during an ongoing merge"),
        Case($(Repository.State.REBASING), "during an ongoing rebase"),
        Case($(Repository.State.REVERTING), "during an ongoing revert"),
        Case($(), state.toString()));

    return String.format("Can't rebase %s", stateName);
  }
}
