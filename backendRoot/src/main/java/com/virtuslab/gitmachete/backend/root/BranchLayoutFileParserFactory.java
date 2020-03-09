package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;

import com.virtuslab.branchlayout.file.BranchLayoutFileParser;

public interface BranchLayoutFileParserFactory {
  BranchLayoutFileParser create(Path path);
}
