package com.virtuslab.branchlayout.impl;

import java.text.MessageFormat;
import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Option;

import lombok.Data;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

@Data
public class BranchLayout implements IBranchLayout {
  private final List<IBranchLayoutEntry> rootBranches;

  @Override
  public Option<IBranchLayoutEntry> findEntryByName(String branchName) {
    return findEntryRecursively(getRootBranches(), e -> e.getName().equals(branchName));
  }

  @Override
  public IBranchLayout slideOut(String branchName) throws BranchLayoutException {
    var entryOption = findEntryByName(branchName);
    if (entryOption.isEmpty()) {
      throw new BranchLayoutException(MessageFormat.format("Branch entry \"{0}\" does not exist", branchName));
    } else if (rootBranches.contains(entryOption.get())) {
      throw new BranchLayoutException("Can not slide out root branch entry");
    }

    return slideOut(entryOption.get());
  }

  /** @return {@link IBranchLayout} where given {@code entryToSlideOut} is replaced with entries of its subbranches */
  private IBranchLayout slideOut(IBranchLayoutEntry entryToSlideOut) {
    var upstreamEntryOption = findUpstreamEntryForEntry(entryToSlideOut);
    assert upstreamEntryOption.isDefined();
    var upstream = upstreamEntryOption.get();

    var indexInUpstream = upstream.getSubbranches().indexOf(entryToSlideOut);

    var updatedSubbranches = upstream.getSubbranches()
        .removeAt(indexInUpstream)
        .insertAll(indexInUpstream, entryToSlideOut.getSubbranches());

    var updatedUpstream = updateSubbranchesForEntry(upstream, updatedSubbranches);

    return replace(upstream, updatedUpstream);
  }

  /**
   * @return a {@link IBranchLayout} containing all elements of this where the {@code entry} is replaced with
   *         {@code newEntry}
   */
  private IBranchLayout replace(IBranchLayoutEntry entry, IBranchLayoutEntry newEntry) {
    if (rootBranches.contains(entry)) {
      return new BranchLayout(rootBranches.replace(entry, newEntry));
    } else {
      var entryUpstreamOption = findUpstreamEntryForEntry(entry);
      assert entryUpstreamOption.isDefined();
      var upstreamEntry = entryUpstreamOption.get();

      var updatedSubbranches = upstreamEntry.getSubbranches().replace(entry, newEntry);
      var updatedUpstreamEntry = updateSubbranchesForEntry(upstreamEntry, updatedSubbranches);

      return replace(upstreamEntry, updatedUpstreamEntry);
    }
  }

  /** @return a copy of the {@code entry} but with specified {@code subbranches} list */
  private IBranchLayoutEntry updateSubbranchesForEntry(IBranchLayoutEntry entry, List<IBranchLayoutEntry> subbranches) {
    var name = entry.getName();
    var customAnnotation = entry.getCustomAnnotation().getOrNull();
    return new BranchLayoutEntry(name, customAnnotation, subbranches);
  }

  private Option<IBranchLayoutEntry> findUpstreamEntryForEntry(IBranchLayoutEntry entry) {
    return findEntryRecursively(rootBranches, e -> e.getSubbranches().contains(entry));
  }

  /** Recursively traverses the list for an element that satisfies the {@code predicate}. */
  private static Option<IBranchLayoutEntry> findEntryRecursively(
      List<IBranchLayoutEntry> branches,
      Predicate<IBranchLayoutEntry> predicate) {
    for (var branch : branches) {
      if (predicate.test(branch)) {
        return Option.of(branch);
      }

      var result = findEntryRecursively(branch.getSubbranches(), predicate);
      if (result.isDefined()) {
        return result;
      }
    }

    return Option.none();
  }
}
