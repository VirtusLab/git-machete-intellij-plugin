package com.virtuslab.branchrelationfile;

import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BranchRelationFileEntry implements IBranchRelationFileEntry {
  @EqualsAndHashCode.Include private String name;
  private Optional<IBranchRelationFileEntry> upstream = Optional.empty();
  private List<IBranchRelationFileEntry> subbranches = new LinkedList<>();
  private Optional<String> customAnnotation = Optional.empty();

  public BranchRelationFileEntry(
      String name, Optional<IBranchRelationFileEntry> upstream, Optional<String> customAnnotation) {
    this.name = name;
    this.upstream = upstream;
    this.customAnnotation = customAnnotation;
  }

  public BranchRelationFileEntry(IBranchRelationFileEntry branchRelationFileEntry) {
    this.name = branchRelationFileEntry.getName();
    this.customAnnotation = branchRelationFileEntry.getCustomAnnotation();
    this.upstream = branchRelationFileEntry.getUpstream();
    this.subbranches = new LinkedList<>(branchRelationFileEntry.getSubbranches());
  }

  @Override
  public IBranchRelationFileEntry withUpstream(IBranchRelationFileEntry newUpstream) {
    BranchRelationFileEntry newBrfe = new BranchRelationFileEntry(this);
    newBrfe.upstream = Optional.ofNullable(newUpstream);

    return newBrfe;
  }

  @Override
  public void addSubbranch(IBranchRelationFileEntry subbranch) {
    subbranches.add(subbranch);
  }
}
