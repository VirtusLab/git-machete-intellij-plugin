package com.virtuslab.branchlayout.api;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.Nullable;

@AllArgsConstructor
@SuppressWarnings("interning:not.interned") // to allow for `==` comparison in Lombok-generated `withChildren` method
@ToString
@UsesObjectEquals
public class BranchLayoutEntry implements IBranchLayoutEntry {
  @Getter
  private final String name;

  private final @Nullable String customAnnotation;

  @Getter
  @With
  private final List<IBranchLayoutEntry> children;

  @ToString.Include(name = "children") // avoid recursive `toString` calls on children
  private List<String> getChildNames() {
    return children.map(e -> e.getName());
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }
}
