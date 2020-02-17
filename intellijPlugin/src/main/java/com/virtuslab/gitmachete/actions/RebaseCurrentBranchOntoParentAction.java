package com.virtuslab.gitmachete.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;
import com.virtuslab.gitmachete.ui.GitMacheteGraphTableManager;
import git4idea.GitUtil;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public class RebaseCurrentBranchOntoParentAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(RebaseCurrentBranchOntoParentAction.class);
  private final GitMacheteGraphTableManager gitMacheteGraphTableManager;

  public RebaseCurrentBranchOntoParentAction(
      @Nonnull GitMacheteGraphTableManager gitMacheteGraphTableManager) {
    super(
        "Rebase Current Branch Onto Parent",
        "Rebase current branch onto parent",
        AllIcons.Actions.Menu_cut);
    this.gitMacheteGraphTableManager = gitMacheteGraphTableManager;
  }

  @Override
  public void update(@Nonnull AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    // todo prohibit rebase during rebase #79
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
    Project project = anActionEvent.getProject();
    assert project != null;
    GitRepository repository = getRepository(project);
    GitVersion gitVersion = repository.getVcs().getVersion();

    IGitMacheteRepository gitMacheteRepository = gitMacheteGraphTableManager.getRepository();
    Optional<IGitRebaseParameters> gitRebaseParameters =
        computeGitRebaseParameters(gitMacheteRepository);

    if (gitRebaseParameters.isEmpty()) {
      LOG.error("Unable to get rebase parameters");
      return;
    }

    IGitRebaseParameters parameters = gitRebaseParameters.get();
    String currentBranch = parameters.getCurrentBranch().getName();
    String newBase = parameters.getNewBaseCommit().getHash();
    String forkPoint = parameters.getForkPointCommit().getHash();

    new Task.Backgroundable(project, "Rebasing") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        GitRebaseParams params =
            new GitRebaseParams(
                gitVersion,
                currentBranch,
                newBase,
                /*upstream*/ forkPoint,
                /*interactive*/ true,
                /*preserveMerges*/ false);
        GitRebaseUtils.rebase(project, List.of(repository), params, indicator);
      }

      /* todo on success
          Refresh only sync statuses (not whole repository).
          Keep in mind potential changes to commits.
          (eg. commits may get squashed so the graph structure changes)
      */
    }.queue();
  }

  @Nonnull
  private Optional<IGitRebaseParameters> computeGitRebaseParameters(
      IGitMacheteRepository gitMacheteRepository) {
    Optional<IGitMacheteBranch> gitMacheteCurrentBranch;
    Optional<IGitRebaseParameters> gitRebaseParameters = Optional.empty();
    try {
      gitMacheteCurrentBranch = gitMacheteRepository.getCurrentBranchIfManaged();
      if (gitMacheteCurrentBranch.isPresent()) {
        gitRebaseParameters = Optional.of(gitMacheteCurrentBranch.get().computeRebaseParameters());
      }
    } catch (GitMacheteException | GitException e) {
      LOG.error("Unable to compute rebase parameters", e);
    }

    return gitRebaseParameters;
  }

  /**
   * The visibility predicate {@link
   * com.virtuslab.gitmachete.ui.GitMacheteContentProvider.GitMacheteVisibilityPredicate} performs
   * {@link com.intellij.openapi.vcs.ProjectLevelVcsManager#checkVcsIsActive(java.lang.String)}
   * which is true when the specified VCS is used by at least one module in the project. Therefore
   * it is guaranteed that while the Git Machete plugin tab is visible, a git repository exists.
   */
  protected GitRepository getRepository(Project project) {
    // todo handle multiple repositories #64
    Iterator<GitRepository> iterator = GitUtil.getRepositories(project).iterator();
    assert iterator.hasNext();
    return iterator.next();
  }
}
