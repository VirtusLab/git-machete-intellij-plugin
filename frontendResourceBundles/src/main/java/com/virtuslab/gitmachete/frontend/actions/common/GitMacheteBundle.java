package com.virtuslab.gitmachete.frontend.actions.common;

import java.util.ResourceBundle;

import org.checkerframework.checker.i18nformatter.qual.I18nMakeFormat;
import org.jetbrains.annotations.PropertyKey;

public final class GitMacheteBundle {
  public static final String BUNDLE = "GitMacheteBundle";
  private static final ResourceBundle INSTANCE = ResourceBundle.getBundle(BUNDLE);

  private GitMacheteBundle() {}

  @I18nMakeFormat
  public static String getString(@PropertyKey(resourceBundle = BUNDLE) String key) {
    return INSTANCE.getString(key);
  }
}
