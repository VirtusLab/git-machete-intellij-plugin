package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import io.vavr.collection.Set;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;

/** Each implementing class must have a public parameterless constructor. */
public interface IGitMacheteRepositoryFactory {
  IGitMacheteRepository create(
      Path mainDirectoryPath,
      Path gitDirectoryPath,
      IBranchLayout branchLayout) throws GitMacheteException;

  Option<String> inferUpstreamForLocalBranch(
      Path mainDirectoryPath,
      Path gitDirectoryPath,
      Set<String> managedBranchNames,
      String localBranchName) throws GitMacheteException;

  IGitMacheteRepository discover(
      Path mainDirectoryPath,
      Path gitDirectoryPath) throws GitMacheteException;
}
