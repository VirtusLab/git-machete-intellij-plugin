package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.IBranchLayoutSaver;

/** Each implementing class must have a public parameterless constructor. */
public interface IBranchLayoutSaverFactory {
  IBranchLayoutSaver create(Path macheteFilePath)
      throws GitMacheteException;
}
