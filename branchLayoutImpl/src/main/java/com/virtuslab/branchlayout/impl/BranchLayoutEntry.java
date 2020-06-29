package com.virtuslab.branchlayout.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

@AllArgsConstructor
@SuppressWarnings("interning:not.interned") // to allow for `==` comparison in Lombok-generated `withSubentries` method
@ToString
@UsesObjectEquals
public class BranchLayoutEntry implements IBranchLayoutEntry {
  @Getter(onMethod_ = {@Override})
  private final String name;

  private final @Nullable String customAnnotation;

  @Getter(onMethod_ = {@Override})
  @With(onMethod_ = {@Override})
  private final List<IBranchLayoutEntry> subentries;

  @ToString.Include(name = "subentries") // avoid recursive `toString` calls on subentries
  private List<String> getSubentryNames() {
    return subentries.map(e -> e.getName());
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }
}
