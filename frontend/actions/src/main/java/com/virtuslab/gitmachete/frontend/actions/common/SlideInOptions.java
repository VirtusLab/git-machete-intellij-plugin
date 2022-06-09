package com.virtuslab.gitmachete.frontend.actions.common;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.tainting.qual.Untainted;

@Getter
public class SlideInOptions {

  @Untainted
  private String name;
  @Accessors(fluent = true)
  private Boolean shouldReattach;

  public SlideInOptions(@Untainted String name, boolean shouldReattach) {
    this.name = name;
    this.shouldReattach = shouldReattach;
  }
}
