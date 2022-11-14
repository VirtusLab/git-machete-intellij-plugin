package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import java.util.Collections;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import lombok.val;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class CheckoutBackgroundable extends Task.Backgroundable {

  private final String toBeCheckedOutEntryName;

  private final GitRepository gitRepository;

  public CheckoutBackgroundable(Project project, String title, String toBeCheckedOutEntryName, GitRepository gitRepository) {
    super(project, title);
    this.toBeCheckedOutEntryName = toBeCheckedOutEntryName;
    this.gitRepository = gitRepository;

  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    if (myProject == null) {
      return;
    }
    doCheckout(myProject, indicator, toBeCheckedOutEntryName, gitRepository);
  }

  @UIThreadUnsafe
  public static void doCheckout(Project project, ProgressIndicator indicator, String branchToCheckoutName,
      GitRepository gitRepository) {
    val uiHandler = new GitBranchUiHandlerImpl(project, indicator);
    new GitBranchWorker(project, Git.getInstance(), uiHandler)
        .checkout(branchToCheckoutName, /* detach */ false, Collections.singletonList(gitRepository));
  }
}
