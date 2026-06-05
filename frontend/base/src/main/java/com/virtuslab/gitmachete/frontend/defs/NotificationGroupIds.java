package com.virtuslab.gitmachete.frontend.defs;

public final class NotificationGroupIds {
  private NotificationGroupIds() {}

  // Registered via `<notificationGroup id="GitMachete" ...>` in `META-INF/plugin.xml`.
  // This is the group passed to `NotificationGroupManager.getInstance().getNotificationGroup(...)`
  // when creating user-facing balloons, replacing the deprecated `VcsNotifier.STANDARD_NOTIFICATION`
  // group which is reserved for git4idea's own notifications.
  public static final String GIT_MACHETE = "GitMachete";
}
