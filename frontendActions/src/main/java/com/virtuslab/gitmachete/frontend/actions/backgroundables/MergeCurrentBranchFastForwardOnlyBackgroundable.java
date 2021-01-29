package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import lombok.Getter;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

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
  protected @I18nFormat({}) String getOperationName() {
    return getString("action.GitMachete.MergeCurrentBranchFastForwardOnlyBackgroundable.operation-name");
  }

  @Override
  @UIThreadUnsafe
  protected @Nullable GitLineHandler createGitLineHandler() {
    var handler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.MERGE);
    handler.addParameters("--ff-only");
    handler.addParameters(targetBranchName);
    return handler;
  }
}
