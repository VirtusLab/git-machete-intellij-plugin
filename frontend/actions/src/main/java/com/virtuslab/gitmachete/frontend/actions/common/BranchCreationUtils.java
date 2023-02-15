package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;

import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
public final class BranchCreationUtils {

  private BranchCreationUtils() {}

  /**
   * @return true, when the branch has been created in the allowed time, otherwise false
   */
  @UIThreadUnsafe
  public static boolean waitForCreationOfLocalBranch(GitRepository gitRepository, String branchName) {
    try {
      // Usually just 3 attempts are enough
      val MAX_SLEEP_DURATION = 16384;
      var sleepDuration = 64;
      while (gitRepository.getBranches().findLocalBranch(branchName) == null && sleepDuration <= MAX_SLEEP_DURATION) {
        //noinspection BusyWait
        Thread.sleep(sleepDuration);
        sleepDuration *= 2;
      }
    } catch (InterruptedException e) {
      VcsNotifier.getInstance(gitRepository.getProject()).notifyWeakError(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideInBackgroundable.notification.message.wait-interrupted")
              .fmt(branchName));
    }

    if (gitRepository.getBranches().findLocalBranch(branchName) == null) {
      VcsNotifier.getInstance(gitRepository.getProject()).notifyWeakError(/* displayId */ null,
          /* title */ "",
          getString("action.GitMachete.BaseSlideInBackgroundable.notification.message.timeout").fmt(branchName));
      return false;
    }
    return true;
  }
}
