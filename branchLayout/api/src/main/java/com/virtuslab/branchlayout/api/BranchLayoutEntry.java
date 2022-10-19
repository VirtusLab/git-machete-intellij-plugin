package com.virtuslab.branchlayout.api;

import java.util.Comparator;
import java.util.Objects;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *  Class that encapsulates a single branch and a list of its children. <br>
 *  Two {@code BranchLayoutEntry} objects are equal when their names, their custom annotations
 *  and their children are <b>all</b> equal (recursively checked for children).
 *  Parents are <b>not</b> taken into account in equality checks to avoid infinite recursion.
 */
@SuppressWarnings("interning:not.interned") // to allow for `==` comparison in Lombok-generated `with...` methods
@ToString
public final class BranchLayoutEntry {
  @Getter
  private final String name;

  @Getter
  private final @Nullable String customAnnotation;

  @Getter
  @With(AccessLevel.PRIVATE)
  private final @Nullable BranchLayoutEntry parent;

  @Getter
  @With
  private final List<BranchLayoutEntry> children;

  // Extracted a separate, all-args constructor solely for the sake of Lombok to implement `withChildren`
  @SuppressWarnings("nullness:argument") // to allow for passing not fully initialized `this` to `withParent`
  private BranchLayoutEntry(String name, @Nullable String customAnnotation,
      @Nullable BranchLayoutEntry parent,
      List<BranchLayoutEntry> children) {
    this.name = name;
    this.customAnnotation = customAnnotation;
    this.parent = parent;
    this.children = children.map(child -> child.withParent(this));
  }

  public BranchLayoutEntry(String name, @Nullable String customAnnotation, List<BranchLayoutEntry> children) {
    this(name, customAnnotation, /* parent */ null, children);
  }

  @ToString.Include(name = "children") // avoid recursive `toString` calls on children
  private List<String> getChildNames() {
    return children.map(e -> e.name);
  }

  @ToString.Include(name = "parent") // avoid recursive `toString` calls on parent
  private @Nullable String getParentName() {
    return parent != null ? parent.name : null;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BranchLayoutEntry)) {
      return false;
    } else {
      val otherEntry = (BranchLayoutEntry) other;
      val areNamesSame = this.name.equals(otherEntry.name);
      val areCustomAnnotationsSame = Objects.equals(this.customAnnotation, otherEntry.customAnnotation);

      if (areNamesSame && areCustomAnnotationsSame && this.children.size() == otherEntry.children.size()) {
        val entryNameComparator = Comparator.comparing(BranchLayoutEntry::getName);

        val sortedSelfEntries = this.children.sorted(entryNameComparator);
        val sortedOtherEntries = otherEntry.children.sorted(entryNameComparator);

        return sortedSelfEntries.zip(sortedOtherEntries)
            .forAll(entryTuple -> entryTuple._1.equals(entryTuple._2));
      }

      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, customAnnotation, children);
  }
}
