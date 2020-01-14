package com.virtuslab.branchrelationfile;

import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileBranchEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.*;

@RequiredArgsConstructor
@Getter
public class BranchRelationFileBranchEntry implements IBranchRelationFileBranchEntry {
  @NonNull private String name;
  @Setter @NonNull private Optional<IBranchRelationFileBranchEntry> upstream;
  private List<IBranchRelationFileBranchEntry> subbranches = new LinkedList<>();
  @NonNull private Optional<String> customAnnotation;

  public void addSubbranch(IBranchRelationFileBranchEntry subbranch) {
    subbranches.add(subbranch);
  }

  public void slideOut() throws BranchRelationFileException {
    if (upstream.isEmpty()) throw new BranchRelationFileException("Can not slide out root branch");

    var upBranch = upstream.get();
    int indexOfThatBranch = upBranch.getSubbranches().indexOf(this);

    for (var childBranch : subbranches) {
      childBranch.setUpstream(upstream);
      upBranch.getSubbranches().add(indexOfThatBranch, childBranch);
    }

    upBranch.getSubbranches().remove(this);
  }
}
