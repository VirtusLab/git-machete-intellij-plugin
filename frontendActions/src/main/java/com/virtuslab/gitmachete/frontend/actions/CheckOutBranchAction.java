package com.virtuslab.gitmachete.frontend.actions;

import java.util.Iterator;
import java.util.List;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;

public class CheckOutBranchAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(CheckOutBranchAction.class);

  public CheckOutBranchAction() {}

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
  }

  /**
   * Expects DataKeys:
   * <ul>
   *  <li>{@link CommonDataKeys#PROJECT}</li>
   *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
   * </ul>
   */
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    String selectedBranchName = anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME);
    if (selectedBranchName == null) {
      LOG.error("Branch to check out was not given");
      return;
    }

    Project project = anActionEvent.getProject();
    assert project != null;
    GitRepository repository = getRepository(project);

    new Task.Backgroundable(project, "Checking out") {
      @Override
      public void run(ProgressIndicator indicator) {
        new GitBranchWorker(project, Git.getInstance(),
            new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator))
                .checkout(selectedBranchName, /* detach */ false, List.of(repository));
      }
      // TODO (#95): on success, refresh only indication of the current branch
    }.queue();
  }

  protected GitRepository getRepository(Project project) {
    // TODO (#64): handle multiple repositories
    Iterator<GitRepository> iterator = GitUtil.getRepositories(project).iterator();
    assert iterator.hasNext();
    return iterator.next();
  }
}
