package com.virtuslab.gitmachete.frontend.actions.common;

import org.checkerframework.checker.tainting.qual.Untainted;

public class SlideInOptions {

  @Untainted
  private String name;
  private boolean reattach;

  public SlideInOptions(@Untainted String name, boolean shouldReattach) {
    this.name = name;
    this.reattach = shouldReattach;
  }

  public @Untainted String getName() {
    return name;
  }

  public boolean shouldReattach() {
    return reattach;
  }
}
