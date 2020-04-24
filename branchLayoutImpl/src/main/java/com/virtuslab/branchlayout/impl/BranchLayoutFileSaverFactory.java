package com.virtuslab.branchlayout.impl;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.IBranchLayoutSaver;
import com.virtuslab.branchlayout.api.IBranchLayoutSaverFactory;

public class BranchLayoutFileSaverFactory implements IBranchLayoutSaverFactory {
  @Override
  public IBranchLayoutSaver create(Path filePath) {
    return new BranchLayoutFileSaver(filePath);
  }
}
