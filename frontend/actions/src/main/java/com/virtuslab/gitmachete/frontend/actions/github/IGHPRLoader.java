package com.virtuslab.gitmachete.frontend.actions.github;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IGHPRLoader {

  @UIThreadUnsafe
  void run();
}
