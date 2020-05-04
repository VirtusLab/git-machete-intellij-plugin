package com.virtuslab.logger;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.GuiUtils;

public final class LoggingUtils {
  private LoggingUtils() {}

  static void showErrorDialog(String message, String title) {
    GuiUtils.invokeLaterIfNeeded(() -> Messages.showErrorDialog(message, title), ModalityState.NON_MODAL);
  }

  public static void showErrorDialog(String message) {
    showErrorDialog(message, "Something Went Wrong...");
  }
}
