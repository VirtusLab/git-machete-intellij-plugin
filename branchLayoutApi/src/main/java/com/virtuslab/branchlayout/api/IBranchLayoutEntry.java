package com.virtuslab.branchlayout.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IBranchLayoutEntry {
  String getName();

  List<IBranchLayoutEntry> getSubbranches();

  Option<String> getCustomAnnotation();
}
