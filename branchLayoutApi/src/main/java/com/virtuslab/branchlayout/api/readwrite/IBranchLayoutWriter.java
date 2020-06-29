package com.virtuslab.branchlayout.api.readwrite;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IBranchLayoutWriter {
  void write(Path path, IBranchLayout branchLayout, boolean backupOldLayout) throws BranchLayoutException;
}
