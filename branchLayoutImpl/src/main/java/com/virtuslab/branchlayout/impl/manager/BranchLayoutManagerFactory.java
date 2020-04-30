package com.virtuslab.branchlayout.impl.manager;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.manager.IBranchLayoutManager;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutManagerFactory;

public class BranchLayoutManagerFactory implements IBranchLayoutManagerFactory {
  @Override
  public IBranchLayoutManager create(Path path) {
    return new BranchLayoutManager(path);
  }
}
