package com.virtuslab.branchlayout.api;

import java.util.Comparator;
import java.util.Objects;

import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *  Two {@code IBranchLayout} objects are equal when their root entries (after sorting by name) are equal.
 *
 *  @see IBranchLayoutEntry
 */
public interface IBranchLayout {
  List<IBranchLayoutEntry> getRootEntries();

  @Nullable
  IBranchLayoutEntry findEntryByName(String branchName);

  @Nullable
  IBranchLayoutEntry findNextEntry(String branchName);

  @Nullable
  IBranchLayoutEntry findPreviousEntry(String branchName);

  default boolean hasEntry(String branchName) {
    return findEntryByName(branchName) != null;
  }

  IBranchLayout slideIn(String parentBranchName, IBranchLayoutEntry entryToSlideIn)
      throws EntryDoesNotExistException, EntryIsDescendantOfException;

  IBranchLayout slideOut(String branchName);

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(@FindDistinct IBranchLayout self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IBranchLayout)) {
      return false;
    } else {
      val otherLayout = (IBranchLayout) other;
      if (self.getRootEntries().size() == otherLayout.getRootEntries().size()) {
        val entryNameComparator = Comparator.comparing(IBranchLayoutEntry::getName);

        val sortedSelfRootEntries = self.getRootEntries().sorted(entryNameComparator);
        val sortedOtherRootEntries = otherLayout.getRootEntries().sorted(entryNameComparator);

        return sortedSelfRootEntries.zip(sortedOtherRootEntries)
            .forAll(rootEntryTuple -> rootEntryTuple._1.equals(rootEntryTuple._2));
      }

      return false;
    }
  }

  static int defaultHashCode(IBranchLayout self) {
    return Objects.hash(self.getRootEntries());
  }
}
