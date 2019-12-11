package com.virtuslab.gitmachete.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.VcsShortCommitDetails;
import git4idea.branch.GitRebaseParams;
import git4idea.rebase.GitCommitEditingAction;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import java.util.List;
import javax.annotation.Nonnull;

/* this is only a PoC, todo: implement this feature */
public class GitInteractiveRebaseAction extends GitCommitEditingAction {
  @Override
  public void update(@Nonnull AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    prohibitRebaseDuringRebase(anActionEvent, "rebase", false);
  }

  @Override
  public void actionPerformedAfterChecks(@Nonnull AnActionEvent anActionEvent) {
    VcsShortCommitDetails commit = getSelectedCommit(anActionEvent);
    Project project = anActionEvent.getProject();
    GitRepository repository = getRepository(anActionEvent);

    String branch =
        repository.getCurrentBranchName() != null
            ? repository.getCurrentBranchName()
            : repository.getCurrentRevision();
    String targetBranch = "null";

    new Task.Backgroundable(project, "Rebasing") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        GitRebaseParams params =
            new GitRebaseParams(
                branch,
                targetBranch,
                commit.getParents().get(0).asString(),
                true,
                false); // interactive and do not preserve merges
        GitRebaseUtils.rebase(project, List.of(repository), params, indicator);
      }
    }.queue();
  }

  @Nonnull
  @Override
  protected String getFailureTitle() {
    return "Couldn't Start Rebase";
  }
}
