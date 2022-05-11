package com.virtuslab.gitmachete.frontend.actions.common;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class ActionUtils {
  private ActionUtils() {}

  public static String createRefspec(String sourceBranchFullName, String targetBranchFullName, boolean allowNonFastForward) {
    return (allowNonFastForward ? "+" : "") + sourceBranchFullName + ":" + targetBranchFullName;
  }

  public static String getQuotedStringOrCurrent(@Nullable String string) {
    return string != null ? "'${string}'" : "current";
  }
}
