package com.virtuslab.gitmachete.backendroot;

import java.nio.file.Path;

import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;

public interface BranchRelationFileFactory {
	IBranchRelationFile create(Path pathToBranchRelationFile) throws BranchRelationFileException;
}
