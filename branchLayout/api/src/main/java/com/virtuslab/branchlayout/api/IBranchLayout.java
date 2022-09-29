package com.virtuslab.branchlayout.api;

import java.util.Comparator;

import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *  Two IBranchLayout objects are equal when their root entries are equal.
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

  default boolean equals(IBranchLayout other) {
    if (this.getRootEntries().size() == other.getRootEntries().size()) {
      val entryNameComparator = Comparator.comparing(IBranchLayoutEntry::getName);

      val sortedThisRootEntries = this.getRootEntries()
          .sorted(entryNameComparator);
      val sortedOtherRootEntries = other.getRootEntries()
          .sorted(entryNameComparator);

      return sortedThisRootEntries.zip(sortedOtherRootEntries)
          .forAll(rootEntryTuple -> rootEntryTuple._1.equals(rootEntryTuple._2));
    }

    return false;
  }
}
