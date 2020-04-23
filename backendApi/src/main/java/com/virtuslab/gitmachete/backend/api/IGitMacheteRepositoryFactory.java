package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.IBranchLayout;

/** Each implementing class must have a public parameterless constructor. */
public interface IGitMacheteRepositoryFactory {
  IGitMacheteRepository create(Path mainDirectoryPath, Path gitDirectoryPath, IBranchLayout branchLayout)
      throws GitMacheteException;
}
