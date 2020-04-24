package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.IBranchLayoutParser;

/** Each implementing class must have a public parameterless constructor. */
public interface IBranchLayoutParserFactory {
  IBranchLayoutParser create(Path macheteFilePath)
      throws GitMacheteException;
}
