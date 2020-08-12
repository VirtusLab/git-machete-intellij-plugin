package com.virtuslab.gitmachete.frontend.resourcebundles;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nMakeFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.PropertyKey;

public final class GitMacheteBundle {
  public static final String BUNDLE = "GitMacheteBundle";
  private static final ResourceBundle INSTANCE = ResourceBundle.getBundle(BUNDLE);

  private GitMacheteBundle() {}

  /**
   * A more restrictive version of {@link MessageFormat#format(String, Object...)}.
   * Since each parameter must be a non-null {@link String},
   * we can capture the unintended parameter types (like Vavr {@code Option}) more easily during the compilation
   * (currently, this is realized with {@code FenumChecker}, see config/checker/FORBIDDEN-METHODS.astub).
   *
   * @param format as in {@link MessageFormat#format(String, Object...)}
   * @param args as in {@link MessageFormat#format(String, Object...)},
   *             but each parameter must be a non-null {@link String} and not just a nullable {@link Object}
   * @return the formatted string
   */
  @SuppressWarnings("regexp") // to allow for the sole invocation of MessageFormat.format in the project
  public static String format(@I18nFormatFor("#2") String format, String... args) {
    return MessageFormat.format(format, (@Nullable Object[]) args);
  }

  @I18nMakeFormat
  public static String getString(@PropertyKey(resourceBundle = BUNDLE) String key) {
    return INSTANCE.getString(key);
  }
}
