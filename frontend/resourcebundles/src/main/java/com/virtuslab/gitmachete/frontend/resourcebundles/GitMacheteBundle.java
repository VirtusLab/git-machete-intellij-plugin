package com.virtuslab.gitmachete.frontend.resourcebundles;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nMakeFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.common.value.qual.MinLen;
import org.jetbrains.annotations.PropertyKey;

public final class GitMacheteBundle {
  public static final char MNEMONIC = 0x1B;
  public static final String BUNDLE = "GitMacheteBundle";
  private static final ResourceBundle instance = ResourceBundle.getBundle(BUNDLE);

  private GitMacheteBundle() {}

  /**
   * A more restrictive version of {@link MessageFormat#format(String, Object...)}.
   * Since each parameter must be a non-null {@link String},
   * we can capture the unintended parameter types (like {@code io.vavr.control.Option}) more easily during the build
   * (this is realized with ArchUnit; see the test against {@code Option#toString} in the top-level project).
   * Note that we consciously use the name "fmt" instead of "format"
   * to avoid an accidental use of {@link String#format(String, Object...)}
   * and emphasize the need to use the {@link lombok.experimental.ExtensionMethod} annotation.
   *
   * @param format as in {@link MessageFormat#format(String, Object...)}
   * @param args as in {@link MessageFormat#format(String, Object...)},
   *             but each parameter must be a non-null {@link String} and not just a nullable {@link Object}
   * @return the formatted string
   */
  public static @PolyTainted String fmt(@PolyTainted @I18nFormatFor("#2") String format, String @MinLen(1)... args) {
    return MessageFormat.format(format, (@Nullable Object[]) args);
  }

  @I18nMakeFormat
  public static String getString(@PropertyKey(resourceBundle = BUNDLE) String key) {
    String string = instance.getString(key);
    return replaceMnemonicAmpersand(string);
  }

  @I18nMakeFormat
  public static @Untainted String getNonHtmlString(@PropertyKey(resourceBundle = BUNDLE) String key) {
    return instance.getString(key);
  }

  /**
   * This method has been inspired by {@link com.intellij.BundleBase#replaceMnemonicAmpersand}.
   * It is required to correctly process the mnemonics.
   */
  private static String replaceMnemonicAmpersand(String value) {
    if (value.indexOf('&') < 0 || value.indexOf(MNEMONIC) >= 0) {
      return value;
    }

    StringBuilder builder = new StringBuilder();
    boolean macMnemonic = value.contains("&&");
    boolean mnemonicAdded = false;
    int i = 0;
    while (i < value.length()) {
      char c = value.charAt(i);
      if (c == '\\') {
        if (i < value.length() - 1 && value.charAt(i + 1) == '&') {
          builder.append('&');
          i++;
        } else {
          builder.append(c);
        }
      } else if (c == '&') {
        if (i < value.length() - 1 && value.charAt(i + 1) == '&') {
          if (SystemInfoRt.IS_MAC) {
            if (!mnemonicAdded) {
              mnemonicAdded = true;
              builder.append(MNEMONIC);
            }
          }
          i++;
        } else if (!SystemInfoRt.IS_MAC || !macMnemonic) {
          if (!mnemonicAdded) {
            mnemonicAdded = true;
            builder.append(MNEMONIC);
          }
        }
      } else {
        builder.append(c);
      }
      i++;
    }
    return builder.toString();
  }
}

/**
 * This method has been inspired by {@link com.intellij.openapi.util.SystemInfoRt}.
 * It is required for {@link GitMacheteBundle#replaceMnemonicAmpersand(String)}.
 */
class SystemInfoRt {
  public static final String OS_NAME = System.getProperty("os.name");
  private static final String LOWERCASE_OS_NAME = OS_NAME.toLowerCase(Locale.ENGLISH);
  public static final boolean IS_MAC = LOWERCASE_OS_NAME.startsWith("mac");
}
