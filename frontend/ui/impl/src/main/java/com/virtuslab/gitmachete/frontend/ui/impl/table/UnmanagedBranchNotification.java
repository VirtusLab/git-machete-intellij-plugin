package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.vcs.VcsNotifier;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;

import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
public class UnmanagedBranchNotification extends Notification {

  @Getter
  private final String branchName;

  UnmanagedBranchNotification(String branchName) {
    super(VcsNotifier.STANDARD_NOTIFICATION.getDisplayId(),
        getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.text").fmt(branchName),
        NotificationType.INFORMATION);
    this.branchName = branchName;
  }
}
