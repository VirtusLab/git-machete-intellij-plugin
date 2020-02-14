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
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public class GitInteractiveRebaseAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(GitInteractiveRebaseAction.class);
  private final GitMacheteGraphTableManager gitMacheteGraphTableManager;

  public GitInteractiveRebaseAction(
      @Nonnull GitMacheteGraphTableManager gitMacheteGraphTableManager) {
    super("Update Current Branch", "Update current branch", AllIcons.Actions.Menu_cut);
    this.gitMacheteGraphTableManager = gitMacheteGraphTableManager;
  }

  @Override
  public void update(@Nonnull AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    // todo prohibit rebase during rebase
  }

  @Override
  public final void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
    actionPerformedAfterChecks(anActionEvent);
  }

  public void actionPerformedAfterChecks(@Nonnull AnActionEvent anActionEvent) {
    Project project = anActionEvent.getProject();
    GitRepository repository = getRepository(project);
    GitVersion gitVersion = repository.getVcs().getVersion();

    IGitMacheteRepository gitMacheteRepository = gitMacheteGraphTableManager.getRepository();
    Optional<IGitRebaseParameters> gitRebaseParameters =
        getGitRebaseParameters(gitMacheteRepository);

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
                /*preserveMerges*/ false); // interactive and do not preserve merges
        assert project != null;
        GitRebaseUtils.rebase(project, List.of(repository), params, indicator);
      }

      @Override
      public void onSuccess() {
        // todo only refresh sync statuses (but commits may get squashed)
        gitMacheteGraphTableManager.updateRepository();
        gitMacheteGraphTableManager.refreshUI();
      }
    }.queue();
  }

  @NotNull
  private Optional<IGitRebaseParameters> getGitRebaseParameters(
      IGitMacheteRepository gitMacheteRepository) {
    Optional<IGitMacheteBranch> gitMacheteCurrentBranch;
    Optional<IGitRebaseParameters> gitRebaseParameters = Optional.empty();
    try {
      gitMacheteCurrentBranch = gitMacheteRepository.getCurrentBranchIfManaged();
      if (gitMacheteCurrentBranch.isPresent()) {
        gitRebaseParameters = Optional.of(gitMacheteCurrentBranch.get().computeRebaseParameters());
      }
    } catch (GitMacheteException | GitException e) {
      e.printStackTrace();
      LOG.error("Unable to compute rebase parameters", e);
    }

    return gitRebaseParameters;
  }

  protected GitRepository getRepository(Project project) {
    // todo handle multiple repositories
    return GitUtil.getRepositories(project).iterator().next();
  }
}
