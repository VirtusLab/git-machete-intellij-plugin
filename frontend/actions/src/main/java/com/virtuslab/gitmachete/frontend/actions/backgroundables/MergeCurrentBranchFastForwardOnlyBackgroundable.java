package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class MergeCurrentBranchFastForwardOnlyBackgroundable extends GitCommandUpdatingCurrentBranchBackgroundable {

  private final IBranchReference targetBranch;

  @Override
  public String getTargetBranchName() {
    return targetBranch.getName();
  }

  public MergeCurrentBranchFastForwardOnlyBackgroundable(
      GitRepository gitRepository,
      IBranchReference targetBranch) {
    super(gitRepository, getString("action.GitMachete.BaseFastForwardMergeToParentAction.task-title"));
    this.targetBranch = targetBranch;
  }

  @Override
  protected LambdaLogger log() {
    return LOG;
  }

  @Override
  protected @I18nFormat({}) @Untainted String getOperationName() {
    return getNonHtmlString("action.GitMachete.MergeCurrentBranchFastForwardOnlyBackgroundable.operation-name");
  }

  @Override
  @UIThreadUnsafe
  protected @Nullable GitLineHandler createGitLineHandler() {
    val handler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.MERGE);
    handler.addParameters("--ff-only");
    handler.addParameters(targetBranch.getFullName());
    return handler;
  }
}
