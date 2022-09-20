package com.virtuslab.branchlayout.api;

import java.util.Comparator;

import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *  Class that encapsulates a single branch and a list of its children. <br>
 *  Two IBranchLayoutEntry objects are equal when their names, their custom annotations
 *  and their children are ALL equal (recursively checked for children).
 */
public interface IBranchLayoutEntry {
  String getName();

  List<IBranchLayoutEntry> getChildren();

  IBranchLayoutEntry withChildren(List<IBranchLayoutEntry> newChildren);

  @Nullable
  String getCustomAnnotation();

  default boolean equals(IBranchLayoutEntry other) {
    val areNamesSame = this.getName().equals(other.getName());
    val thisCustomAnnotation = this.getCustomAnnotation();
    val otherCustomAnntation = other.getCustomAnnotation();

    var areCustomAnnotationsSame = thisCustomAnnotation == null && otherCustomAnntation == null;

    if (thisCustomAnnotation != null && otherCustomAnntation != null) {
      areCustomAnnotationsSame = thisCustomAnnotation.equals(otherCustomAnntation);
    }

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
