package com.virtuslab.branchlayout.impl;

import java.util.Optional;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;

@RequiredArgsConstructor
@ToString
public class BranchLayoutEntry extends BaseBranchLayoutEntry {
  @Getter
  private final String name;
  @Nullable
  private final String customAnnotation;
  @Getter
  private final List<BaseBranchLayoutEntry> subbranches;

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }
}
