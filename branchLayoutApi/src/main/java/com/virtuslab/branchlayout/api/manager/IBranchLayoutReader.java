package com.virtuslab.branchlayout.api.manager;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IBranchLayoutReader extends IIndentDefining {
  IBranchLayout read() throws BranchLayoutException;
}
