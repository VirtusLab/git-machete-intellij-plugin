package com.virtuslab.branchlayout.api;

import java.util.Comparator;

import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class implementing this interface is reference equality
 */
public interface IBranchLayoutEntry {
  String getName();

  List<IBranchLayoutEntry> getChildren();

  IBranchLayoutEntry withChildren(List<IBranchLayoutEntry> newChildren);

  @Nullable
  String getCustomAnnotation();

  default boolean equals(IBranchLayoutEntry other) {
    val areNamesSame = this.getName().equals(other.getName());
    val areCustomAnnotationsSame = this.getCustomAnnotation().equals(other.getCustomAnnotation());

    if (areNamesSame && areCustomAnnotationsSame && this.getChildren().size() == other.getChildren().size()) {
      val entryNameComparator = Comparator.comparing(IBranchLayoutEntry::getName);

      val sortedThisEntries = this.getChildren()
          .sorted(entryNameComparator);
      val sortedOtherEntries = other.getChildren()
          .sorted(entryNameComparator);

      return sortedThisEntries.zip(sortedOtherEntries)
          .forAll(entryTuple -> entryTuple._1.equals(entryTuple._2));
    }

    return false;
  }
}
