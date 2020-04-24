package com.virtuslab.branchlayout.impl;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.IBranchLayoutParser;
import com.virtuslab.branchlayout.api.IBranchLayoutParserFactory;

public class BranchLayoutFileParserFactory implements IBranchLayoutParserFactory {
  @Override
  public IBranchLayoutParser create(Path filePath) {
    return new BranchLayoutFileParser(filePath);
  }
}
