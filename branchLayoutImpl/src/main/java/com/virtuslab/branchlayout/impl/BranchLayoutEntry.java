package com.virtuslab.branchlayout.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

@RequiredArgsConstructor
@ToString
public class BranchLayoutEntry implements IBranchLayoutEntry {
  @Getter
  private final String name;
  @Nullable
  private final String customAnnotation;
  @Getter
  private final List<IBranchLayoutEntry> subentries;

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }
}
