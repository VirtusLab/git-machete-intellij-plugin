package com.virtuslab.branchlayout.api;

import java.util.Comparator;
import java.util.Objects;

import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.Getter;
import lombok.val;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *  Two {@code BranchLayout} objects are equal when their root entries (after sorting by name) are equal.
 *
 *  @see BranchLayoutEntry
 */
public class BranchLayout {

  @Getter
  private final List<BranchLayoutEntry> rootEntries;

  private final List<BranchLayoutEntry> allEntries;

  private final Map<String, BranchLayoutEntry> entryByName;

  public BranchLayout(List<BranchLayoutEntry> rootEntries) {
    this.rootEntries = rootEntries;
    this.allEntries = rootEntries.flatMap(BranchLayout::collectEntriesRecursively);
    this.entryByName = allEntries.toMap(entry -> Tuple.of(entry.getName(), entry));
  }

  private static List<BranchLayoutEntry> collectEntriesRecursively(BranchLayoutEntry entry) {
    return entry.getChildren().flatMap(BranchLayout::collectEntriesRecursively).prepend(entry);
  }

  public @Nullable BranchLayoutEntry getEntryByName(String branchName) {
    return entryByName.get(branchName).getOrNull();
  }

  public boolean hasEntry(String branchName) {
    return getEntryByName(branchName) != null;
  }

  public boolean isEntryDuplicated(String branchName) {
    val numberOfEntriesForBranchName = allEntries
        .count(entry -> entry.getName().equals(branchName));
    return numberOfEntriesForBranchName > 1;
  }

  public @Nullable BranchLayoutEntry findNextEntry(String branchName) {
    val entriesOrderedList = allEntries.map(BranchLayoutEntry::getName);
    val currentIndex = entriesOrderedList.indexOf(branchName);
    if (currentIndex > -1 && currentIndex + 1 < entriesOrderedList.length()) {
      @LTLengthOf("entriesOrderedList") int nextIndex = currentIndex + 1;
      return getEntryByName(entriesOrderedList.get(nextIndex));
    }
    return null;
  }

  public @Nullable BranchLayoutEntry findPreviousEntry(String branchName) {
    val entriesOrderedList = allEntries.map(BranchLayoutEntry::getName);
    val currentIndex = entriesOrderedList.indexOf(branchName);
    if (currentIndex > 0 && currentIndex < entriesOrderedList.length()) {
      @LTLengthOf("entriesOrderedList") int previousIndex = currentIndex - 1;
      return getEntryByName(entriesOrderedList.get(previousIndex));
    }
    return null;
  }

  public BranchLayout slideOut(String branchName) {
    return new BranchLayout(rootEntries.flatMap(rootEntry -> slideOut(rootEntry, branchName)));
  }

  private List<BranchLayoutEntry> slideOut(BranchLayoutEntry entry, String entryNameToSlideOut) {
    val newChildren = entry.getChildren().flatMap(child -> slideOut(child, entryNameToSlideOut));
    if (entry.getName().equals(entryNameToSlideOut)) {
      return newChildren;
    } else {
      return List.of(entry.withChildren(newChildren));
    }
  }

  public BranchLayout slideIn(String parentBranchName, BranchLayoutEntry entryToSlideIn)
      throws EntryDoesNotExistException, EntryIsDescendantOfException {
    val parentEntry = getEntryByName(parentBranchName);
    if (parentEntry == null) {
      throw new EntryDoesNotExistException("Parent branch entry '${parentBranchName}' does not exist");
    }
    val entry = getEntryByName(entryToSlideIn.getName());
    val entryAlreadyExists = entry != null;

    if (entry != null && isDescendant(/* presumedAncestor */ entry, /* presumedDescendant */ parentEntry)) {
      throw new EntryIsDescendantOfException(
          "Entry '${parentEntry.getName()}' is a descendant of entry '${entryToSlideIn.getName()}'");
    }

    val newRootEntries = entryAlreadyExists
        ? removeEntry(/* branchLayout */ this, entryToSlideIn.getName())
        : rootEntries;
    return new BranchLayout(newRootEntries.map(rootEntry -> slideIn(rootEntry, entryToSlideIn, parentEntry)));
  }

  private static boolean isDescendant(BranchLayoutEntry presumedAncestor, BranchLayoutEntry presumedDescendant) {
    if (presumedAncestor.getChildren().contains(presumedDescendant)) {
      return true;
    }
    return presumedAncestor.getChildren().exists(e -> isDescendant(e, presumedDescendant));
  }

  private static List<BranchLayoutEntry> removeEntry(BranchLayout branchLayout, String branchName) {
    val rootEntries = branchLayout.getRootEntries();
    if (rootEntries.map(e -> e.getName()).exists(name -> name.equals(branchName))) {
      return rootEntries.reject(e -> e.getName().equals(branchName));
    } else {
      return removeEntry(rootEntries, branchName);
    }
  }

  private static List<BranchLayoutEntry> removeEntry(List<BranchLayoutEntry> entries, String branchName) {
    return entries.reject(e -> e.getName().equals(branchName))
        .map(e -> e.withChildren(removeEntry(e.getChildren(), branchName)));
  }

  private static BranchLayoutEntry slideIn(
      BranchLayoutEntry entry,
      BranchLayoutEntry entryToSlideIn,
      BranchLayoutEntry parent) {
    val children = entry.getChildren();
    if (entry.getName().equals(parent.getName())) {
      return entry.withChildren(children.append(entryToSlideIn));
    } else {
      return entry.withChildren(children.map(child -> slideIn(child, entryToSlideIn, parent)));
    }
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BranchLayout)) {
      return false;
    } else {
      val otherLayout = (BranchLayout) other;
      if (this.rootEntries.size() == otherLayout.rootEntries.size()) {
        val entryNameComparator = Comparator.comparing(BranchLayoutEntry::getName);

        val sortedSelfRootEntries = this.rootEntries.sorted(entryNameComparator);
        val sortedOtherRootEntries = otherLayout.rootEntries.sorted(entryNameComparator);

        return sortedSelfRootEntries.zip(sortedOtherRootEntries)
            .forAll(rootEntryTuple -> rootEntryTuple._1.equals(rootEntryTuple._2));
      }

      return false;
    }
  }

  @Override
  public final int hashCode() {
    return Objects.hash(rootEntries);
  }
}
