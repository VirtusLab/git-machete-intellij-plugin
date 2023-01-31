package com.virtuslab.gitmachete.frontend.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.util.ThrowableRunnable;
import lombok.SneakyThrows;
import org.checkerframework.checker.guieffect.qual.UI;

public final class WriteActionUtils {

  private WriteActionUtils() {}

  // We don't provide asynchronous (non-blocking) variant since it turned out prone to race conditions.
  public static <E extends Throwable> void blockingRunWriteActionOnUIThread(@UI ThrowableRunnable<E> action) {
    ApplicationManager.getApplication().invokeAndWait(getWriteActionRunnable(action));
  }

  private static <E extends Throwable> @UI Runnable getWriteActionRunnable(@UI ThrowableRunnable<E> action) {
    return new Runnable() {
      @Override
      @SneakyThrows
      public void run() {
        WriteAction.run(action);
      }
    };
  }

}
