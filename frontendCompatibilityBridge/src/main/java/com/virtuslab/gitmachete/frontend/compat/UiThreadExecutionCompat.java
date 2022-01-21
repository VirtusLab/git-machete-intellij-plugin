package com.virtuslab.gitmachete.frontend.compat;

import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.GuiUtils;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UI;

public final class UiThreadExecutionCompat {
  private UiThreadExecutionCompat() {}

  // TODO (#772): switch to ModalityUiUtil.invokeLaterIfNeeded once we no longer support 2021.2
  @SuppressWarnings("removal")
  @SafeEffect
  public static void invokeLaterIfNeeded(ModalityState modalityState, @UI Runnable runnable) {
    GuiUtils.invokeLaterIfNeeded(runnable, modalityState);
  }
}
