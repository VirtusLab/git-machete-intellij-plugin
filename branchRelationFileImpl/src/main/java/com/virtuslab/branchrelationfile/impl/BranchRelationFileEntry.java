package com.virtuslab.branchrelationfile.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import lombok.Data;

import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;

@Data
public class BranchRelationFileEntry implements IBranchRelationFileEntry {
  private final String name;
  private final IBranchRelationFileEntry upstream;
  private final String customAnnotation;
  private final List<IBranchRelationFileEntry> subbranches = new LinkedList<>();

  // can't be c'tors coz Lombok wouldn't then generate any c'tor itself
  public static IBranchRelationFileEntry of(
      IBranchRelationFileEntry branchRelationFileEntry, IBranchRelationFileEntry upstream) {
    String customAnnotation = branchRelationFileEntry.getCustomAnnotation().orElse(null);
    var result = new BranchRelationFileEntry(branchRelationFileEntry.getName(), upstream, customAnnotation);
    result.getSubbranches().addAll(branchRelationFileEntry.getSubbranches());
    return result;
  }

  public static IBranchRelationFileEntry of(IBranchRelationFileEntry branchRelationFileEntry) {
    var upstream = branchRelationFileEntry.getUpstream().orElse(null);
    return BranchRelationFileEntry.of(branchRelationFileEntry, upstream);
  }

  @Override
  public Optional<IBranchRelationFileEntry> getUpstream() {
    return Optional.ofNullable(upstream);
  }

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }

  @Override
  public IBranchRelationFileEntry withUpstream(IBranchRelationFileEntry newUpstream) {
    return BranchRelationFileEntry.of(/* branchRelationFileEntry */ this, newUpstream);
  }

  @Override
  public void addSubbranch(IBranchRelationFileEntry subbranch) {
    subbranches.add(subbranch);
  }
}
