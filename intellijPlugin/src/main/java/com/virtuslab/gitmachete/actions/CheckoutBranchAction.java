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

public class CheckoutBranchAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(CheckoutBranchAction.class);

  public CheckoutBranchAction() {}

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

    new Task.Backgroundable(project, "Rebasing") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        new GitBranchWorker(
                project,
                Git.getInstance(),
                new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator))
            .checkout("call-ws", false, List.of(repository));
      }

      /* todo on success
          Refresh only sync statuses (not whole repository).
          Keep in mind potential changes to commits.
          (eg. commits may get squashed so the graph structure changes)
      */
    }.queue();

    /*int row = rowAtPoint(e.getPoint());

    int col = columnAtPoint(e.getPoint());

              /*JOptionPane.showMessageDialog(
                      null, "Value in the cell clicked :" + " " + getValueAt(row, col).toString());

    JOptionPane.showMessageDialog(null, "bbbbb");*/

  }

  protected GitRepository getRepository(Project project) {
    // todo handle multiple repositories #64
    Iterator<GitRepository> iterator = GitUtil.getRepositories(project).iterator();
    assert iterator.hasNext();
    return iterator.next();
  }
}
