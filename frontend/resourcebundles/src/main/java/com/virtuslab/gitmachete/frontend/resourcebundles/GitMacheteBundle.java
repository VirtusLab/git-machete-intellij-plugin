package com.virtuslab.gitmachete.frontend.resourcebundles;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nMakeFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.common.value.qual.MinLen;
import org.jetbrains.annotations.PropertyKey;

public final class GitMacheteBundle {
  public static final String BUNDLE = "GitMacheteBundle";
  private static final ResourceBundle instance = ResourceBundle.getBundle(BUNDLE);

  private GitMacheteBundle() {}

  /**
   * A more restrictive version of {@link MessageFormat#format(String, Object...)}.
   * Since each parameter must be a non-null {@link String},
   * we can capture the unintended parameter types (like {@code io.vavr.control.Option}) more easily during the build
   * (this is realized with ArchUnit; see the test against {@code Option#toString} in the top-level project).
   *
   * @param format as in {@link MessageFormat#format(String, Object...)}
   * @param args as in {@link MessageFormat#format(String, Object...)},
   *             but each parameter must be a non-null {@link String} and not just a nullable {@link Object}
   * @return the formatted string
   */
  public static @PolyTainted String format(@PolyTainted @I18nFormatFor("#2") String format, String @MinLen(1)... args) {
    return MessageFormat.format(format, (@Nullable Object[]) args);
  }

  @I18nMakeFormat
  public static String getString(@PropertyKey(resourceBundle = BUNDLE) String key) {
    return instance.getString(key);
  }

  @I18nMakeFormat
  public static @Untainted String getNonHtmlString(@PropertyKey(resourceBundle = BUNDLE) String key) {
    return instance.getString(key);
  }
}
