package com.virtuslab.branchlayout.api;

import java.util.Comparator;
import java.util.Objects;

import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *  Class that encapsulates a single branch and a list of its children. <br>
 *  Two {@code BranchLayoutEntry} objects are equal when their names, their custom annotations
 *  and their children are ALL equal (recursively checked for children).
 */
@AllArgsConstructor // for Lombok-generated `withChildren`
@SuppressWarnings("interning:not.interned") // to allow for `==` comparison in Lombok-generated `withChildren` method
@ToString
public final class BranchLayoutEntry {
  @Getter
  private final String name;

  @Getter
  private final @Nullable String customAnnotation;

  @Getter
  private @Nullable BranchLayoutEntry parent = null;

  @Getter
  @With
  private final List<BranchLayoutEntry> children;

  public BranchLayoutEntry(String name, @Nullable String customAnnotation, List<BranchLayoutEntry> children) {
    this.name = name;
    this.customAnnotation = customAnnotation;
    this.children = children;

    // Note: since the class is final, `this` is already @Initialized at this point.
    // We're thus safe to store `this` in other objects' fields.

    for (val child : children) {
      child.parent = this;
    }
  }

  @ToString.Include(name = "children") // avoid recursive `toString` calls on children
  private List<String> getChildNames() {
    return children.map(e -> e.getName());
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
