package com.virtuslab.branchrelationfile;

import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.*;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BranchRelationFileEntry implements IBranchRelationFileEntry, Cloneable {
  @EqualsAndHashCode.Include private String name;
  @Setter private Optional<IBranchRelationFileEntry> upstream = Optional.empty();
  private List<IBranchRelationFileEntry> subbranches = new LinkedList<>();
  private Optional<String> customAnnotation = Optional.empty();

  public BranchRelationFileEntry(
      String name, Optional<IBranchRelationFileEntry> upstream, Optional<String> customAnnotation) {
    this.name = name;
    this.upstream = upstream;
    this.customAnnotation = customAnnotation;
  }

  public void addSubbranch(IBranchRelationFileEntry subbranch) {
    subbranches.add(subbranch);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    BranchRelationFileEntry clone = (BranchRelationFileEntry) super.clone();

    clone.upstream = Optional.ofNullable(upstream.orElse(null));
    clone.customAnnotation = Optional.ofNullable(customAnnotation.orElse(null));
    clone.subbranches = new LinkedList<>(subbranches);

    return clone;
  }
}
