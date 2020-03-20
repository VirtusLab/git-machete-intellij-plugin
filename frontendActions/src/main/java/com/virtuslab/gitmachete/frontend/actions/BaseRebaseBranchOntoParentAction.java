package com.virtuslab.gitmachete.frontend.actions;

import static io.vavr.API.$;
import static io.vavr.API.Case;

import java.util.Iterator;
import java.util.Optional;

import javax.swing.Icon;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.repo.GitRepository;
import io.vavr.API;
import io.vavr.control.Try;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 * </ul>
 */
public abstract class BaseRebaseBranchOntoParentAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(BaseRebaseBranchOntoParentAction.class);

  public BaseRebaseBranchOntoParentAction(String text, String actionDescription, Icon icon) {
    super(text, actionDescription, icon);
  }

  public BaseRebaseBranchOntoParentAction() {}

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    anActionEvent.getPresentation().setEnabled(true);
    prohibitRebaseDuringProhibitingState(anActionEvent);
  }

  @Override
  public final void actionPerformed(AnActionEvent anActionEvent) {
    actionPerformedAfterChecks(anActionEvent);
  }

  public abstract void actionPerformedAfterChecks(AnActionEvent anActionEvent);

  IGitMacheteRepository getMacheteRepository(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY);
    assert gitMacheteRepository != null : "Can't get gitMacheteRepository";

    return gitMacheteRepository;
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

  Optional<IGitRebaseParameters> deriveGitRebaseOntoParentParameters(IGitMacheteRepository repository,
      BaseGitMacheteBranch branchToRebase) {
    if (branchToRebase == null) {
      return Optional.empty();
    }

    return Try.of(() -> Optional.of(repository.deriveParametersForRebaseOntoParent(branchToRebase)))
        .onFailure(e -> LOG.error("Unable to compute rebase parameters", e))
        .getOrElse(() -> Optional.empty());
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

  private void prohibitRebaseDuringProhibitingState(AnActionEvent anActionEvent) {
    Repository.State state = getIdeaRepository(anActionEvent).getState();
    var presentation = anActionEvent.getPresentation();
    if (state != Repository.State.NORMAL && presentation.isEnabledAndVisible()) {
      var message = getProhibitedStateMessage(state);
      presentation.setEnabled(false);
      presentation.setDescription(message);
    }
  }

  private String getProhibitedStateMessage(Repository.State state) {

    var stateName = API.Match(state).of(
        Case($(Repository.State.GRAFTING), "cherry-pick"),
        Case($(Repository.State.DETACHED), "detached head"),
        Case($(Repository.State.MERGING), "merge"),
        Case($(Repository.State.REBASING), "rebase"),
        Case($(Repository.State.REVERTING), "revert"),
        Case($(), state.toString()));

    return String.format("Can't rebase during %s", stateName);
  }
}
