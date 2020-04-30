package com.virtuslab.branchlayout.api.manager;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IBranchLayoutWriter {
  void write(IBranchLayout branchLayout, boolean backupOldLayout) throws BranchLayoutException;
}
