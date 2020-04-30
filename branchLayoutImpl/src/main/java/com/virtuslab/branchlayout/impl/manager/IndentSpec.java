package com.virtuslab.branchlayout.impl.manager;

import lombok.Data;
import org.checkerframework.checker.index.qual.Positive;

@Data
public class IndentSpec {

  public static final char DEFAULT_INDENT_CHARACTER = ' ';
  @Positive
  public static final int DEFAULT_INDENT_WIDTH = 2;

  public static final IndentSpec DEFAULT_SPEC = new IndentSpec(DEFAULT_INDENT_CHARACTER, DEFAULT_INDENT_WIDTH);

  private final char indentCharacter;
  @Positive
  private final int indentWidth;
}
