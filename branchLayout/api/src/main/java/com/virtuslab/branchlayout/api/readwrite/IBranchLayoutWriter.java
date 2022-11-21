package com.virtuslab.branchlayout.api.readwrite;

import java.io.IOException;
import java.io.OutputStream;

import io.vavr.collection.List;

import com.virtuslab.branchlayout.api.BranchLayout;

public interface IBranchLayoutWriter {
  void write(OutputStream outputStream, BranchLayout branchLayout, IndentSpec indentSpec) throws IOException;

  IndentSpec deriveIndentSpec(List<String> lines);
}
