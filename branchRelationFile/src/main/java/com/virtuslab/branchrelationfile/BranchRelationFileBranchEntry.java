package com.virtuslab.branchrelationfile;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.*;

@RequiredArgsConstructor
@Getter
public class BranchRelationFileBranchEntry {
  @NonNull private String name;
  @NonNull private Optional<BranchRelationFileBranchEntry> upstream;
  private List<BranchRelationFileBranchEntry> subbranches = new LinkedList<>();
  @NonNull private Optional<String> customAnnotation;

  void addSubbranch(BranchRelationFileBranchEntry subbranch) {
    subbranches.add(subbranch);
  }

  public void slideOut() throws BranchRelationFileException {
    if (upstream.isEmpty()) throw new BranchRelationFileException("Can not slide out root branch");

    var upBranch = upstream.get();
    int indexOfThatBranch = upBranch.subbranches.indexOf(this);

    for (var childBranch : subbranches) {
      childBranch.upstream = upstream;
      upBranch.subbranches.add(indexOfThatBranch, childBranch);
    }

    upBranch.subbranches.remove(this);
  }
}
