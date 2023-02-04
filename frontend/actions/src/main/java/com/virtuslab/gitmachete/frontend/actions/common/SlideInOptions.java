package com.virtuslab.gitmachete.frontend.actions.common;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public class SlideInOptions {
  private final String name;
  @Accessors(fluent = true)
  private final Boolean shouldReattach;
  private final String customAnnotation;

  public SlideInOptions(String name, boolean shouldReattach, String customAnnotation) {
    this.name = name;
    this.shouldReattach = shouldReattach;
    this.customAnnotation = customAnnotation;
  }
}
