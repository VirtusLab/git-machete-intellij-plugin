package com.virtuslab.gitmachete.frontend.actions.hooks;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Objects;

import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
@ExtensionMethod(Objects.class)
public final class PostSlideOutHookExecutor extends BaseGit4IdeaHookExecutor {

  @UIThreadUnsafe
  public PostSlideOutHookExecutor(GitRepository gitRepository) {
    super("machete-post-slide-out", gitRepository);
  }

  @UIThreadUnsafe
  public boolean executeHookFor(@Nullable String parentBranch, String slidOutBranch, List<String> childBranches) {
    String failureNotificationTitle = getString(
        "action.GitMachete.PostSlideOutHookExecutor.notification.title.fail");
    int timeoutSeconds = 20 + childBranches.length() * 10;
    val args = List.of(parentBranch.requireNonNullElse(""), slidOutBranch).appendAll(childBranches).toJavaArray(String[]::new);

    return executeGit4IdeaHook(failureNotificationTitle, timeoutSeconds, args);
  }

  @Override
  protected LambdaLogger log() {
    return LOG;
  }
}
