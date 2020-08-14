package com.virtuslab.gitmachete.backend.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IBranchReference;

@Getter
@RequiredArgsConstructor
@ToString
public abstract class BaseBranchReference implements IBranchReference {
  private final String name;
  private final String fullName;

  @Override
  public final boolean equals(@Nullable Object other) {
    return IBranchReference.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IBranchReference.defaultHashCode(this);
  }
}
