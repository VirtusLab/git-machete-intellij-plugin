package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitLocalBranch;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
public final class BranchCreationUtils {

  private BranchCreationUtils() {}

  @UIThreadUnsafe
  public static @Nullable GitLocalBranch findLocalBranch(GitRepository gitRepository, String branchName) {
    return gitRepository.getBranches().findLocalBranch(branchName);
  }

  /**
   * @return true, when the branch has been created in the allowed time, otherwise false
   */
  @UIThreadUnsafe
  public static boolean waitForCreationOfLocalBranch(GitRepository gitRepository, String branchName) {
    try {
      // Usually just 3 attempts are enough
      val MAX_SLEEP_DURATION = 16384;
      var sleepDuration = 64;
      while (findLocalBranch(gitRepository, branchName) == null && sleepDuration <= MAX_SLEEP_DURATION) {
        Thread.sleep(sleepDuration);
        sleepDuration *= 2;
      }
    } catch (InterruptedException e) {
      VcsNotifier.getInstance(gitRepository.getProject()).notifyWeakError(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideInBackgroundable.notification.message.wait-interrupted")
              .fmt(branchName));
    }

    if (findLocalBranch(gitRepository, branchName) == null) {
      VcsNotifier.getInstance(gitRepository.getProject()).notifyWeakError(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideInBackgroundable.notification.message.timeout").fmt(branchName));
      return false;
    }
    return true;
  }
}
