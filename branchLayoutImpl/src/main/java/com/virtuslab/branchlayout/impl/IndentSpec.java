package com.virtuslab.branchlayout.impl;

import lombok.Data;
import org.checkerframework.checker.index.qual.Positive;

@Data
public class IndentSpec {
  public static final char TAB = '\t';
  public static final char SPACE = ' ';

  private final char indentCharacter;

  private final @Positive int indentWidth;
}
