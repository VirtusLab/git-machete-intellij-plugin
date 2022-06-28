package com.virtuslab.gitmachete.frontend.compat;

import com.intellij.openapi.application.ModalityState;
import com.intellij.util.ModalityUiUtil;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UI;

public final class UiThreadExecutionCompat {
  private UiThreadExecutionCompat() {}

  @SafeEffect
  public static void invokeLaterIfNeeded(ModalityState modalityState, @UI Runnable runnable) {
    ModalityUiUtil.invokeLaterIfNeeded(modalityState, runnable);
  }
}
