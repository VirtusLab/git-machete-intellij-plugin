package com.virtuslab.gitmachete.frontend.actions.common;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class ActionUtils {
  private ActionUtils() {}

  public static String getQuotedStringOrCurrent(@Nullable String string) {
    return string != null ? "'${string}'" : "current";
  }
}
