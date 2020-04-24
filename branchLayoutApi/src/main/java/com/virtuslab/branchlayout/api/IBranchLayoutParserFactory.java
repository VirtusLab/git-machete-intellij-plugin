package com.virtuslab.branchlayout.api;

import java.nio.file.Path;

/** Each implementing class must have a public parameterless constructor. */
public interface IBranchLayoutParserFactory {
  IBranchLayoutParser create(Path path) throws BranchLayoutException;
}
