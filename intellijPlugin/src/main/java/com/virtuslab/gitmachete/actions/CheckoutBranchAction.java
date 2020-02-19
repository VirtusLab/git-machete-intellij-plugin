package com.virtuslab.gitmachete.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Setter;

public class CheckoutBranchAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(CheckoutBranchAction.class);

  @Setter private static String nameOfBranchToCheckout; // issue #91

  public CheckoutBranchAction() {}

  @Override
  public void update(@Nonnull AnActionEvent anActionEvent) {
    super.update(anActionEvent);
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
    if (nameOfBranchToCheckout == null) {
      LOG.error("Branch to checkout was not given");
      return;
    }

    Project project = anActionEvent.getProject();
    assert project != null;
    GitRepository repository = getRepository(project);

    new Task.Backgroundable(project, "Checking out") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        new GitBranchWorker(
                project,
                Git.getInstance(),
                new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator))
            .checkout(nameOfBranchToCheckout, /*detach*/ false, List.of(repository));
      }
      /* todo on success
          Refresh only indication of current branch
      */
    }.queue();
  }

  protected GitRepository getRepository(Project project) {
    // todo handle multiple repositories #64
    Iterator<GitRepository> iterator = GitUtil.getRepositories(project).iterator();
    assert iterator.hasNext();
    return iterator.next();
  }
}
