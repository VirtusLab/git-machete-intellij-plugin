package com.virtuslab.branchlayout.api.manager;

public interface IBranchLayoutManager {
  IBranchLayoutReader getReader();
  IBranchLayoutWriter getWriter();
}
