package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.IBranchLayoutParser;
import com.virtuslab.branchlayout.impl.BranchLayoutFileParser;
import com.virtuslab.gitmachete.backend.api.IBranchLayoutParserFactory;

public class BranchLayoutParserFactory implements IBranchLayoutParserFactory {
  @Override
  public IBranchLayoutParser create(Path macheteFilePath) {
    return new BranchLayoutFileParser(macheteFilePath);
  }
}
