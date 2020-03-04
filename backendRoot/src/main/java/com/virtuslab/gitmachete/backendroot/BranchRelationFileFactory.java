package com.virtuslab.gitmachete.backendroot;

import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import java.nio.file.Path;

public interface BranchRelationFileFactory {
  IBranchRelationFile create(Path pathToBranchRelationFile) throws BranchRelationFileException;
}
