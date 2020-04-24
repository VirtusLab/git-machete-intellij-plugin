package com.virtuslab.branchlayout.api;

public interface IBranchLayoutSaver {
  void save(IBranchLayout branchLayout) throws BranchLayoutException;

  void setBackupOldFile(boolean b);
}
