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
}
