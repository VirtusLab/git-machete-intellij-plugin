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
@AllArgsConstructor
@SuppressWarnings("interning:not.interned") // to allow for `==` comparison in Lombok-generated `withChildren` method
@ToString
public class BranchLayoutEntry {
  @Getter
  private final String name;

  @Getter
  private final @Nullable String customAnnotation;

  @Getter
  @With
  private final List<BranchLayoutEntry> children;

  @ToString.Include(name = "children") // avoid recursive `toString` calls on children
  private List<String> getChildNames() {
    return children.map(e -> e.getName());
  }

  @Override
  public final boolean equals(@Nullable Object other) {
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
  public final int hashCode() {
    return Objects.hash(name, customAnnotation, children);
  }
}
