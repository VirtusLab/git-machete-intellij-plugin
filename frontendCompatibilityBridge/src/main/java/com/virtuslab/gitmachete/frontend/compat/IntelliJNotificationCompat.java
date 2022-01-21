package com.virtuslab.gitmachete.frontend.compat;

import java.util.Collection;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

// TODO (#743): remove once we no longer support 2020.2
@SuppressWarnings("removal")
public final class IntelliJNotificationCompat {
  private IntelliJNotificationCompat() {}

  public static void notifySuccess(Project project, String title, String message) {
    VcsNotifier.getInstance(project).notifySuccess(title, message);
  }

  public static void notifyInfo(Project project, String title, String message) {
    VcsNotifier.getInstance(project).notifyInfo(title, message);
  }

  public static void notifyWarning(Project project, String title, String message) {
    VcsNotifier.getInstance(project).notifyWarning(title, message);
  }

  public static void notifyWeakError(Project project, String title, String message) {
    VcsNotifier.getInstance(project).notifyWeakError(title, message);
  }

  public static void notifyError(Project project, String title, String message) {
    VcsNotifier.getInstance(project).notifyError(title, message);
  }

  @UIThreadUnsafe
  public static void localChangesWouldBeOverwrittenHelper_showErrorNotification(
      Project project,
      VirtualFile root,
      String operationName,
      Collection<String> relativeFilePaths) {

    LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project, root, operationName, relativeFilePaths);
  }
}
