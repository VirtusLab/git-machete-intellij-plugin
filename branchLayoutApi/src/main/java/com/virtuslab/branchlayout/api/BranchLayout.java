package com.virtuslab.branchlayout.api;

import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.Getter;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;

@UsesObjectEquals
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
  public Option<IBranchLayoutEntry> findEntryByName(String branchName) {
    return entryByName.get(branchName);
  }

  @Override
  public IBranchLayout slideOut(String branchName) throws EntryDoesNotExistException {
    var entryToSlideOut = findEntryByName(branchName).getOrNull();
    if (entryToSlideOut == null) {
      throw new EntryDoesNotExistException("Branch entry '${branchName}' does not exist");
    }
    return new BranchLayout(rootEntries.flatMap(rootEntry -> slideOut(rootEntry, entryToSlideOut)));
  }

  private List<IBranchLayoutEntry> slideOut(
      @FindDistinct IBranchLayoutEntry entry,
      @FindDistinct IBranchLayoutEntry entryToSlideOut) {
    var children = entry.getChildren();
    if (entry == entryToSlideOut) {
      return children;
    } else {
      return List.of(entry.withChildren(children.flatMap(child -> slideOut(child, entryToSlideOut))));
    }
  }

  @Override
  public IBranchLayout slideIn(String parentBranchName, IBranchLayoutEntry entryToSlideIn)
      throws EntryDoesNotExistException, EntryIsDescendantOfException {
    var parentEntry = findEntryByName(parentBranchName).getOrNull();
    if (parentEntry == null) {
      throw new EntryDoesNotExistException("Parent branch entry '${parentBranchName}' does not exist");
    }
    var entry = findEntryByName(entryToSlideIn.getName());
    var entryAlreadyExists = entry.isDefined();

    if (entry.map(e -> isDescendant(/* presumedAncestor */ e, /* presumedDescendant */ parentEntry)).getOrElse(false)) {
      throw new EntryIsDescendantOfException(
          "Entry '${parentEntry.getName()}' is a descendant of entry '${entryToSlideIn.getName()}'",
          /* descendant */ parentEntry,
          /* ancestor */ entryToSlideIn);
    }

    var newRootEntries = entryAlreadyExists
        ? removeEntry(/* branchLayout */ this, entryToSlideIn.getName())
        : rootEntries;
    return new BranchLayout(newRootEntries.map(rootEntry -> slideIn(rootEntry, entryToSlideIn, parentEntry)));
  }

  private static boolean isDescendant(IBranchLayoutEntry presumedAncestor, IBranchLayoutEntry presumedDescendant) {
    if (presumedAncestor.getChildren().contains(presumedDescendant)) {
      return true;
    } else {
      return presumedAncestor.getChildren().exists(e -> isDescendant(e, presumedDescendant));
    }
  }

  private static List<IBranchLayoutEntry> removeEntry(IBranchLayout branchLayout, String branchName) {
    var rootEntries = branchLayout.getRootEntries();
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
    var children = entry.getChildren();
    if (entry.getName().equals(parent.getName())) {
      return entry.withChildren(children.append(entryToSlideIn));
    } else {
      return entry.withChildren(children.map(child -> slideIn(child, entryToSlideIn, parent)));
    }
  }
}
