package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreatedAndDuplicatedAndSkippedBranches<T extends BaseManagedBranchSnapshot> {
  private final List<T> createdBranches;
  private final Set<String> duplicatedBranchNames;
  private final Set<String> skippedBranchNames;

  public CreatedAndDuplicatedAndSkippedBranches<T> withExtraDuplicatedBranch(String duplicatedBranchName) {
    return new CreatedAndDuplicatedAndSkippedBranches<T>(getCreatedBranches(),
        getDuplicatedBranchNames().add(duplicatedBranchName),
        getSkippedBranchNames());
  }

  public CreatedAndDuplicatedAndSkippedBranches<T> withExtraSkippedBranch(String skippedBranchName) {
    return new CreatedAndDuplicatedAndSkippedBranches<T>(getCreatedBranches(), getDuplicatedBranchNames(),
        getSkippedBranchNames().add(skippedBranchName));
  }

  public static <T extends BaseManagedBranchSnapshot> CreatedAndDuplicatedAndSkippedBranches<T> of(List<T> createdBranches,
      Set<String> duplicatedBranchName, Set<String> skippedBranchNames) {
    return new CreatedAndDuplicatedAndSkippedBranches<T>(createdBranches, duplicatedBranchName, skippedBranchNames);
  }

  public static <T extends BaseManagedBranchSnapshot> CreatedAndDuplicatedAndSkippedBranches<T> empty() {
    return new CreatedAndDuplicatedAndSkippedBranches<T>(List.empty(), TreeSet.empty(), TreeSet.empty());
  }

  public static <T extends BaseManagedBranchSnapshot> CreatedAndDuplicatedAndSkippedBranches<T> merge(
      CreatedAndDuplicatedAndSkippedBranches<T> prevResult1, CreatedAndDuplicatedAndSkippedBranches<T> prevResult2) {
    return new CreatedAndDuplicatedAndSkippedBranches<T>(
        prevResult1.getCreatedBranches().appendAll(prevResult2.getCreatedBranches()),
        prevResult1.getDuplicatedBranchNames().addAll(prevResult2.getDuplicatedBranchNames()),
        prevResult1.getSkippedBranchNames().addAll(prevResult2.getSkippedBranchNames()));
  }
}
