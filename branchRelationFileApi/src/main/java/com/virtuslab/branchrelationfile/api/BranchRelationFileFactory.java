package com.virtuslab.branchrelationfile.api;

import java.nio.file.Path;

public interface BranchRelationFileFactory {
  IBranchRelationFile create(Path pathToBranchRelationFile) throws BranchRelationFileException;
}
