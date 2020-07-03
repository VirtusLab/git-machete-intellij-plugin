package com.virtuslab.gitmachete.frontend.actions.common;

import java.util.function.Supplier;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.PropertyKey;

public final class GitMacheteBundle extends DynamicBundle {
  public static final String BUNDLE = "actions.messages.GitMacheteBundle";
  private static final GitMacheteBundle INSTANCE = new GitMacheteBundle();

  private GitMacheteBundle() {
    super(BUNDLE);
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }

  public static Supplier<String> messagePointer(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getLazyMessage(key, params);
  }

  public static String getString(@PropertyKey(resourceBundle = BUNDLE) String key) {
    return message(key);
  }
}
