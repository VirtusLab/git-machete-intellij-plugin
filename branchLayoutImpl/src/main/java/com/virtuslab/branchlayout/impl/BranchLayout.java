package com.virtuslab.branchlayout.impl;

import java.util.Optional;
import java.util.function.Predicate;

import io.vavr.collection.List;
import lombok.Data;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;

@Data
public class BranchLayout implements IBranchLayout {
  private final List<BaseBranchLayoutEntry> rootBranches;

  @Override
  public Optional<BaseBranchLayoutEntry> findEntryByName(String branchName) {
    return findEntryRecursively(getRootBranches(), e -> e.getName().equals(branchName));
  }

  @Override
  public IBranchLayout slideOut(String branchName) throws BranchLayoutException {
    var entryOption = findEntryByName(branchName);
    if (!entryOption.isPresent()) {
      throw new BranchLayoutException("Branch entry '${branchName}' does not exist");
    }
    if (rootBranches.contains(entryOption.get())) {
      throw new BranchLayoutException("Cannot slide out root branch entry");
    }
    return slideOut(entryOption.get());
  }

  /** @return {@link IBranchLayout} where given {@code entryToSlideOut} is replaced with entries of its subbranches */
  private IBranchLayout slideOut(BaseBranchLayoutEntry entryToSlideOut) {
    var upstreamEntryOption = findUpstreamEntryForEntry(entryToSlideOut);
    assert upstreamEntryOption.isPresent();
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
  private IBranchLayout replace(BaseBranchLayoutEntry entry, BaseBranchLayoutEntry newEntry) {
    if (rootBranches.contains(entry)) {
      return new BranchLayout(rootBranches.replace(entry, newEntry));
    } else {
      var entryUpstreamOption = findUpstreamEntryForEntry(entry);
      assert entryUpstreamOption.isPresent();
      var upstreamEntry = entryUpstreamOption.get();

      var updatedSubbranches = upstreamEntry.getSubbranches().replace(entry, newEntry);
      var updatedUpstreamEntry = updateSubbranchesForEntry(upstreamEntry, updatedSubbranches);

      return replace(upstreamEntry, updatedUpstreamEntry);
    }
  }

  /** @return a copy of the {@code entry} but with specified {@code subbranches} list */
  private BaseBranchLayoutEntry updateSubbranchesForEntry(BaseBranchLayoutEntry entry,
      List<BaseBranchLayoutEntry> subbranches) {
    var name = entry.getName();
    var customAnnotation = entry.getCustomAnnotation().orElse(null);
    return new BranchLayoutEntry(name, customAnnotation, subbranches);
  }

  private Optional<BaseBranchLayoutEntry> findUpstreamEntryForEntry(BaseBranchLayoutEntry entry) {
    return findEntryRecursively(rootBranches, e -> e.getSubbranches().contains(entry));
  }

  /** Recursively traverses the list for an element that satisfies the {@code predicate}. */
  private static Optional<BaseBranchLayoutEntry> findEntryRecursively(
      List<BaseBranchLayoutEntry> branches,
      Predicate<BaseBranchLayoutEntry> predicate) {
    for (var branch : branches) {
      if (predicate.test(branch)) {
        return Optional.of(branch);
      }

      var result = findEntryRecursively(branch.getSubbranches(), predicate);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }
}
