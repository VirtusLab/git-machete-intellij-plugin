package com.virtuslab.branchlayout.api;

import java.util.Comparator;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface IBranchLayout {
  List<IBranchLayoutEntry> getRootEntries();

  @Nullable
  IBranchLayoutEntry findEntryByName(String branchName);

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
