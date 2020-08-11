package com.virtuslab.gitmachete.frontend.actions.common;

import io.vavr.control.Option;

public final class ActionUtils {
  private ActionUtils() {}

  public static String getQuotedStringOrCurrent(Option<String> string) {
    return string.map(s -> "'${s}'").getOrElse("current");
  }
}
