package com.virtuslab.branchlayout.api;

import java.util.Optional;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class derived from this one is the reference equality
 */
public abstract class BaseBranchLayoutEntry {
  public abstract String getName();

  public abstract List<BaseBranchLayoutEntry> getSubbranches();

  public abstract Optional<String> getCustomAnnotation();

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return getName().hashCode();
  }
}
