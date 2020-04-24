package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.IBranchLayoutSaver;
import com.virtuslab.branchlayout.impl.BranchLayoutFileSaver;
import com.virtuslab.gitmachete.backend.api.IBranchLayoutSaverFactory;

public class BranchLayoutSaverFactory implements IBranchLayoutSaverFactory {
  @Override
  public IBranchLayoutSaver create(Path macheteFilePath) {
    return new BranchLayoutFileSaver(macheteFilePath);
  }
}
