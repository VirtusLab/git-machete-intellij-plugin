package com.virtuslab.branchrelationfile.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IBranchRelationFile extends Cloneable {
  void saveToFile(boolean backupOldFile) throws IOException, BranchRelationFileException;

  Optional<IBranchRelationFileEntry> findBranchByName(String branchName);

  List<IBranchRelationFileEntry> getRootBranches();

  IBranchRelationFile slideOutBranchAndGetNewBranchRelationFileInstance(String branchName)
      throws BranchRelationFileException, CloneNotSupportedException, IOException;

  IBranchRelationFile slideOutBranchAndGetNewBranchRelationFileInstance(
      IBranchRelationFileEntry relationFileEntry)
      throws BranchRelationFileException, CloneNotSupportedException, IOException;

  Object clone() throws CloneNotSupportedException;
}
