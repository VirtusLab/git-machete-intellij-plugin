package com.virtuslab.branchrelationfile;

import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.Data;

@Data
public class BranchRelationFileEntry implements IBranchRelationFileEntry {
  private final String name;
  private final IBranchRelationFileEntry upstream;
  private final String customAnnotation;
  private final List<IBranchRelationFileEntry> subbranches = new LinkedList<>();

  public static IBranchRelationFileEntry of(
      IBranchRelationFileEntry branchRelationFileEntry, IBranchRelationFileEntry upstream) {
    var result =
        new BranchRelationFileEntry(
            branchRelationFileEntry.getName(),
            upstream,
            branchRelationFileEntry.getCustomAnnotation().orElse(null));
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
