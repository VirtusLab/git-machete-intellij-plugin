package com.virtuslab.branchlayout.api;

import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.Getter;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BranchLayout implements IBranchLayout {

  @Getter
  private final List<IBranchLayoutEntry> rootEntries;

  private final Map<String, IBranchLayoutEntry> entryByName;

  public BranchLayout(List<IBranchLayoutEntry> rootEntries) {
    this.rootEntries = rootEntries;
    this.entryByName = rootEntries.flatMap(BranchLayout::collectEntriesRecursively)
        .toMap(entry -> Tuple.of(entry.getName(), entry));
  }

  private static List<IBranchLayoutEntry> collectEntriesRecursively(IBranchLayoutEntry entry) {
    return entry.getChildren().flatMap(BranchLayout::collectEntriesRecursively).prepend(entry);
  }

  @Override
  public @Nullable IBranchLayoutEntry findEntryByName(String branchName) {
    return entryByName.get(branchName).getOrNull();
  }

  @Override
  public IBranchLayout slideOut(String branchName) {
    return new BranchLayout(rootEntries.flatMap(rootEntry -> slideOut(rootEntry, branchName)));
  }

  private List<IBranchLayoutEntry> slideOut(IBranchLayoutEntry entry, String entryNameToSlideOut) {
    val newChildren = entry.getChildren().flatMap(child -> slideOut(child, entryNameToSlideOut));
    if (entry.getName().equals(entryNameToSlideOut)) {
      return newChildren;
    } else {
      return List.of(entry.withChildren(newChildren));
    }
  }

  @Override
  public IBranchLayout slideIn(String parentBranchName, IBranchLayoutEntry entryToSlideIn)
      throws EntryDoesNotExistException, EntryIsDescendantOfException {
    val parentEntry = findEntryByName(parentBranchName);
    if (parentEntry == null) {
      throw new EntryDoesNotExistException("Parent branch entry '${parentBranchName}' does not exist");
    }
    val entry = findEntryByName(entryToSlideIn.getName());
    val entryAlreadyExists = entry != null;

    if (entry != null && isDescendant(/* presumedAncestor */ entry, /* presumedDescendant */ parentEntry)) {
      throw new EntryIsDescendantOfException(
          "Entry '${parentEntry.getName()}' is a descendant of entry '${entryToSlideIn.getName()}'",
          /* descendant */ parentEntry,
          /* ancestor */ entryToSlideIn);
    }

    val newRootEntries = entryAlreadyExists
        ? removeEntry(/* branchLayout */ this, entryToSlideIn.getName())
        : rootEntries;
    return new BranchLayout(newRootEntries.map(rootEntry -> slideIn(rootEntry, entryToSlideIn, parentEntry)));
  }

  private static boolean isDescendant(IBranchLayoutEntry presumedAncestor, IBranchLayoutEntry presumedDescendant) {
    if (presumedAncestor.getChildren().contains(presumedDescendant)) {
      return true;
    }
    return presumedAncestor.getChildren().exists(e -> isDescendant(e, presumedDescendant));
  }

  private static List<IBranchLayoutEntry> removeEntry(IBranchLayout branchLayout, String branchName) {
    val rootEntries = branchLayout.getRootEntries();
    if (rootEntries.map(e -> e.getName()).exists(name -> name.equals(branchName))) {
      return rootEntries.reject(e -> e.getName().equals(branchName));
    } else {
      return removeEntry(rootEntries, branchName);
    }
  }

  private static List<IBranchLayoutEntry> removeEntry(List<IBranchLayoutEntry> entries, String branchName) {
    return entries.reject(e -> e.getName().equals(branchName))
        .map(e -> e.withChildren(removeEntry(e.getChildren(), branchName)));
  }

  private static IBranchLayoutEntry slideIn(
      IBranchLayoutEntry entry,
      IBranchLayoutEntry entryToSlideIn,
      IBranchLayoutEntry parent) {
    val children = entry.getChildren();
    if (entry.getName().equals(parent.getName())) {
      return entry.withChildren(children.append(entryToSlideIn));
    } else {
      return entry.withChildren(children.map(child -> slideIn(child, entryToSlideIn, parent)));
    }
  }
}
