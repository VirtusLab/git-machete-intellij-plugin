package com.virtuslab.branchlayout.impl;

import java.nio.file.Path;
import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

@CustomLog
@Getter
@RequiredArgsConstructor
@ToString
public class BranchLayout implements IBranchLayout {

  private final List<IBranchLayoutEntry> rootEntries;
  private final Path path;
  private final IndentSpec indentSpec;

  @Override
  public Option<IBranchLayoutEntry> findEntryByName(String branchName) {
    return findEntryRecursively(getRootEntries(), e -> e.getName().equals(branchName));
  }

  @Override
  public IBranchLayout slideOut(String branchName) throws BranchLayoutException {
    var entryOption = findEntryByName(branchName);
    if (entryOption.isEmpty()) {
      throw new BranchLayoutException("Branch entry '${branchName}' does not exist");
    }
    if (rootEntries.contains(entryOption.get())) {
      throw new BranchLayoutException("Cannot slide out root branch entry");
    }
    return slideOut(entryOption.get());
  }

  /** @return {@link IBranchLayout} where the given {@code entryToSlideOut} is replaced with its subentries */
  private IBranchLayout slideOut(IBranchLayoutEntry entryToSlideOut) throws BranchLayoutException {
    LOG.debug(() -> "Entering: entryToSlideOut = '${entryToSlideOut.getName()}'");

    var upstreamEntryOption = findUpstreamEntryForEntry(entryToSlideOut);
    if (upstreamEntryOption.isEmpty()) {
      throw new BranchLayoutException("Can't find upstream for entry ${entryToSlideOut.getName()}");
    }
    var upstream = upstreamEntryOption.get();
    LOG.debug(() -> "Upstream for ${entryToSlideOut.getName()} is ${upstream.getName()}");

    int indexInUpstream = upstream.getSubentries().indexOf(entryToSlideOut);
    LOG.debug(() -> "Entry ${entryToSlideOut.getName()} has index ${indexInUpstream} in its upstream");

    LOG.debug("Removing this entry from upstream and setting this entry's parent as new parent " +
        "for this branch's subentries");

    var updatedSubentries = upstream.getSubentries()
        .removeAt(indexInUpstream)
        .insertAll(indexInUpstream, entryToSlideOut.getSubentries());

    LOG.debug("Following subentries has been updated:");
    entryToSlideOut.getSubentries().forEach(subentry -> LOG.debug(() -> "* ${subentry} (${subentry.getName()})"));

    var updatedUpstream = updateSubentriesForEntry(upstream, updatedSubentries);

    return replace(upstream, updatedUpstream);
  }

  /**
   * @return a {@link IBranchLayout} containing all elements of this where the {@code entry} is replaced with {@code newEntry}
   */
  private IBranchLayout replace(IBranchLayoutEntry oldEntry, IBranchLayoutEntry newEntry) {
    LOG.debug(() -> "Entering: oldEntry = ${oldEntry} (${oldEntry.getName()}), " +
        "newEntry = ${newEntry} (${newEntry.getName()})");
    if (rootEntries.contains(oldEntry)) {
      LOG.debug("Old entry is one of the root entries. Replacing.");
      return new BranchLayout(rootEntries.replace(oldEntry, newEntry), path, indentSpec);
    } else {
      LOG.debug("Old entry is one of subentries. Finding upstream.");

      var entryUpstreamOption = findUpstreamEntryForEntry(oldEntry);
      assert entryUpstreamOption.isDefined() : "Upstream is not defined";
      var upstreamEntry = entryUpstreamOption.get();

      LOG.debug(() -> "Upstream for old entry is ${upstreamEntry} (${upstreamEntry.getName()})");

      LOG.debug("Updating subentries");
      var updatedSubentries = upstreamEntry.getSubentries().replace(oldEntry, newEntry);
      LOG.debug("Updated subentries:");
      updatedSubentries.forEach(entry -> LOG.debug(() -> "* ${entry} (${entry.getName()})"));
      var updatedUpstreamEntry = updateSubentriesForEntry(upstreamEntry, updatedSubentries);

      return replace(upstreamEntry, updatedUpstreamEntry);
    }
  }

  /** @return a copy of the {@code entry} but with the specified {@code subentries} */
  private IBranchLayoutEntry updateSubentriesForEntry(IBranchLayoutEntry entry, List<IBranchLayoutEntry> subentries) {
    var name = entry.getName();
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    return new BranchLayoutEntry(name, customAnnotation, subentries);
  }

  private Option<IBranchLayoutEntry> findUpstreamEntryForEntry(IBranchLayoutEntry entry) {
    return findEntryRecursively(rootEntries, e -> e.getSubentries().contains(entry));
  }

  /** Recursively traverses the list for an element that satisfies the {@code predicate}. */
  private static Option<IBranchLayoutEntry> findEntryRecursively(
      List<IBranchLayoutEntry> entries,
      Predicate<IBranchLayoutEntry> predicate) {
    LOG.debug(() -> "Entering: entries = ${entries}");

    for (var entry : entries) {
      LOG.debug(() -> "Testing entry ${entry} (${entry.getName()})");
      if (predicate.test(entry)) {
        LOG.debug(() -> "Entry ${entry} (${entry.getName()}) satisfies predicate. Returning.");
        return Option.of(entry);
      }

      LOG.debug("Entry not found on this level. Searching in sublevel.");
      var result = findEntryRecursively(entry.getSubentries(), predicate);
      if (result.isDefined()) {
        return result;
      }
    }

    LOG.debug("Entry satisfies the predicate not found on this level nor its sublevels");
    return Option.none();
  }
}
