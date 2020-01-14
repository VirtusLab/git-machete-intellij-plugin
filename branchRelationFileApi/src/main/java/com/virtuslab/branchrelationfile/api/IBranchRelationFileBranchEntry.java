package com.virtuslab.branchrelationfile.api;

import java.util.List;
import java.util.Optional;

public interface IBranchRelationFileBranchEntry {
  String getName();

  Optional<IBranchRelationFileBranchEntry> getUpstream();

  void setUpstream(Optional<IBranchRelationFileBranchEntry> upstream);

  List<IBranchRelationFileBranchEntry> getSubbranches();

  Optional<String> getCustomAnnotation();

  void addSubbranch(IBranchRelationFileBranchEntry subbranch);

  void slideOut() throws BranchRelationFileException;
}
