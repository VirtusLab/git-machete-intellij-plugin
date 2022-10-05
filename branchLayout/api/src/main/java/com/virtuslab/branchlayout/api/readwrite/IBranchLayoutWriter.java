package com.virtuslab.branchlayout.api.readwrite;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IBranchLayoutWriter {
  @UIThreadUnsafe
  void write(Path path, IBranchLayout branchLayout, boolean backupOldLayout) throws BranchLayoutException;
}
