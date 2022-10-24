package com.virtuslab.gitmachete.frontend.actions.common;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public class SlideInOptions {

  private String name;
  @Accessors(fluent = true)
  private Boolean shouldReattach;

  public SlideInOptions(String name, boolean shouldReattach) {
    this.name = name;
    this.shouldReattach = shouldReattach;
  }
}
