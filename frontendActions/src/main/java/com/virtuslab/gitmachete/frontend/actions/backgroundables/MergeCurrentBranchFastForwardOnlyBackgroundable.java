package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.openapi.project.Project;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MergeCurrentBranchFastForwardOnlyBackgroundable extends GitCommandUpdatingCurrentBranchBackgroundable {

  @Getter
  private final String targetBranchName;

  public MergeCurrentBranchFastForwardOnlyBackgroundable(
      Project project,
      GitRepository gitRepository,
      String taskTitle,
      String targetBranchName) {
    super(project, gitRepository, taskTitle);
    this.targetBranchName = targetBranchName;
  }

  @Override
  protected String getOperationName() {
    return "Fast-forward merge";
  }

  @Override
  protected @Nullable GitLineHandler createGitLineHandler() {
    var handler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.MERGE);
    handler.addParameters("--ff-only");
    handler.addParameters(targetBranchName);
    return handler;
  }
}
