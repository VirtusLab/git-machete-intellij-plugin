package com.virtuslab.branchlayout.api;

public interface IBranchLayoutSaver {
  void save(IBranchLayout branchLayout, boolean backupOldLayout) throws BranchLayoutException;
}
