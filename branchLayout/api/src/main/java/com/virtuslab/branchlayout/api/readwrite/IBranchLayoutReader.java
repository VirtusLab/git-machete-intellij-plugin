package com.virtuslab.branchlayout.api.readwrite;

import java.io.InputStream;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;

public interface IBranchLayoutReader {
  BranchLayout read(InputStream inputStream) throws BranchLayoutException;
}
