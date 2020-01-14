package com.virtuslab.branchrelationfile.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IBranchRelationFile {
  void saveToFile() throws IOException, BranchRelationFileException;

  void saveToFile(boolean backupOldFile) throws IOException, BranchRelationFileException;

  Optional<IBranchRelationFileEntry> findBranchByName(String branchName);

  List<IBranchRelationFileEntry> getRootBranches();
}
