package com.virtuslab.branchlayout.api.manager;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IBranchLayoutReader {
  IBranchLayout read(Path path) throws BranchLayoutException;
}
