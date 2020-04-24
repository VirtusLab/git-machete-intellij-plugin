package com.virtuslab.branchlayout.api;

import java.nio.file.Path;

/** Each implementing class must have a public parameterless constructor. */
public interface IBranchLayoutSaverFactory {
  IBranchLayoutSaver create(Path path) throws BranchLayoutException;
}
