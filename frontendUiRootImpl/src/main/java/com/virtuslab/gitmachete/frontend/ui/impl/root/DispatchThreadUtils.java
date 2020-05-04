package com.virtuslab.gitmachete.frontend.ui.impl.root;

import com.intellij.openapi.application.ApplicationManager;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DispatchThreadUtils {
  private DispatchThreadUtils() {}

  public interface IDispatchThreadSupplier<T> {
    @UIEffect
    T get();
  }

  @Nullable
  @SuppressWarnings("guieffect:call.invalid.ui")
  public static <T> T getIfOnDispatchThreadOrNull(IDispatchThreadSupplier<T> supplier) {
    return ApplicationManager.getApplication().isDispatchThread() ? supplier.get() : null;
  }
}
