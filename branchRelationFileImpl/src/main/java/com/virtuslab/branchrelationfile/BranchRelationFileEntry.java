package com.virtuslab.branchrelationfile;

import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BranchRelationFileEntry implements IBranchRelationFileEntry {
  @EqualsAndHashCode.Include @Getter private final String name;
  private final IBranchRelationFileEntry upstream;
  @Getter private final List<IBranchRelationFileEntry> subbranches;
  private final String customAnnotation;

  public BranchRelationFileEntry(
      String name, IBranchRelationFileEntry upstream, String customAnnotation) {
    this.name = name;
    this.upstream = upstream;
    this.customAnnotation = customAnnotation;
    this.subbranches = new LinkedList<>();
  }

  public BranchRelationFileEntry(IBranchRelationFileEntry branchRelationFileEntry) {
    this.name = branchRelationFileEntry.getName();
    this.customAnnotation = branchRelationFileEntry.getCustomAnnotation().orElse(null);
    this.upstream = branchRelationFileEntry.getUpstream().orElse(null);
    this.subbranches = new LinkedList<>(branchRelationFileEntry.getSubbranches());
  }

  public BranchRelationFileEntry(
      IBranchRelationFileEntry branchRelationFileEntry, IBranchRelationFileEntry upstream) {
    this.name = branchRelationFileEntry.getName();
    this.customAnnotation = branchRelationFileEntry.getCustomAnnotation().orElse(null);
    this.upstream = upstream;
    this.subbranches = new LinkedList<>(branchRelationFileEntry.getSubbranches());
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
    return new BranchRelationFileEntry(/* branchRelationFileEntry */ this, newUpstream);
  }

  @Override
  public void addSubbranch(IBranchRelationFileEntry subbranch) {
    subbranches.add(subbranch);
  }
}
