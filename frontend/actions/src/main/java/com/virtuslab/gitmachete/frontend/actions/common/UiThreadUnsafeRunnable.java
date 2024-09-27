package com.virtuslab.gitmachete.frontend.actions.common;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface UiThreadUnsafeRunnable {
  @UIThreadUnsafe
  void run();
}
