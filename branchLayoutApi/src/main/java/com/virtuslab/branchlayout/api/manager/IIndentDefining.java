package com.virtuslab.branchlayout.api.manager;

import org.checkerframework.checker.index.qual.Positive;

public interface IIndentDefining {
  char getIndentCharacter();

  @Positive
  int getIndentWidth();
}
