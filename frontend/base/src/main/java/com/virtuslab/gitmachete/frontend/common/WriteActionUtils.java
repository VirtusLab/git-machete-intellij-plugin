package com.virtuslab.gitmachete.frontend.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.util.ThrowableRunnable;
import lombok.SneakyThrows;
import org.checkerframework.checker.guieffect.qual.UI;

public final class WriteActionUtils {

  private WriteActionUtils() {}

  public static <E extends Throwable> void runWriteActionOnUIThread(@UI ThrowableRunnable<E> action) {
    ApplicationManager.getApplication()
        .invokeLater(new Runnable() {
          @Override
          @SneakyThrows
          public void run() {
            WriteAction.run(action);
          }
        });
  }
}
