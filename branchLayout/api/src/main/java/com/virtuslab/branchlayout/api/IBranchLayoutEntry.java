package com.virtuslab.branchlayout.api;

import java.util.Comparator;
import java.util.Objects;

import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *  Class that encapsulates a single branch and a list of its children. <br>
 *  Two IBranchLayoutEntry objects are equal when their names, their custom annotations
 *  and their children are ALL equal (recursively checked for children).
 */
public interface IBranchLayoutEntry {
  String getName();

  @Nullable
  String getCustomAnnotation();

  List<IBranchLayoutEntry> getChildren();

  IBranchLayoutEntry withChildren(List<IBranchLayoutEntry> newChildren);

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(@FindDistinct IBranchLayoutEntry self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IBranchLayoutEntry)) {
      return false;
    } else {
      val otherEntry = (IBranchLayoutEntry) other;
      val areNamesSame = self.getName().equals(otherEntry.getName());
      val areCustomAnnotationsSame = Objects.equals(self.getCustomAnnotation(), otherEntry.getCustomAnnotation());

      if (areNamesSame && areCustomAnnotationsSame && self.getChildren().size() == otherEntry.getChildren().size()) {
        val entryNameComparator = Comparator.comparing(IBranchLayoutEntry::getName);

        val sortedSelfEntries = self.getChildren().sorted(entryNameComparator);
        val sortedOtherEntries = otherEntry.getChildren().sorted(entryNameComparator);

        return sortedSelfEntries.zip(sortedOtherEntries)
            .forAll(entryTuple -> entryTuple._1.equals(entryTuple._2));
      }

      return false;
    }
  }

  static int defaultHashCode(IBranchLayoutEntry self) {
    return Objects.hash(self.getName(), self.getCustomAnnotation(), self.getChildren());
  }
}
