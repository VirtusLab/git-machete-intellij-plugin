package com.virtuslab.branchlayout.api.manager;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IBranchLayoutReader {
  IBranchLayout read() throws BranchLayoutException;
}
