package com.virtuslab.branchlayout.api.readwrite;

import java.nio.file.Path;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IBranchLayoutReader {
  @UIThreadUnsafe
  BranchLayout read(Path path) throws BranchLayoutException;
}
