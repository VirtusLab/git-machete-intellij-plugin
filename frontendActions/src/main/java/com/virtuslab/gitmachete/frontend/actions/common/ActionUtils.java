package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import lombok.SneakyThrows;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ActionUtils {
  private ActionUtils() {}

  public static String createRefspec(String sourceBranchFullName, String targetBranchFullName, boolean allowNonFastForward) {
    return (allowNonFastForward ? "+" : "") + sourceBranchFullName + ":" + targetBranchFullName;
  }

  public static String getQuotedStringOrCurrent(@Nullable String string) {
    return string != null ? "'${string}'" : "current";
  }

  // TODO (#743): change to non-reflective call once we no longer support 2020.2
  @SneakyThrows
  public static void notifyInfo(Project project, String title, String message) {
    val vcsNotifier = VcsNotifier.getInstance(project);

    try {
      // Proper solution for 2020.3+
      val ternary = VcsNotifier.class.getMethod("notifyInfo", String.class, String.class, String.class);
      ternary.invoke(vcsNotifier, /* displayId */ "git-machete", title, message);
    } catch (ReflectiveOperationException e) {
      // Fallback for 2020.1 and 2020.2
      val binary = VcsNotifier.class.getMethod("notifyInfo", String.class, String.class);
      binary.invoke(vcsNotifier, title, message);
    }
  }
}
