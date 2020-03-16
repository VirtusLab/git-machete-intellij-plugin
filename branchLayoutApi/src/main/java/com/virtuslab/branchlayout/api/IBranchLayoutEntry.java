package com.virtuslab.branchlayout.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IBranchLayoutEntry {
  String getName();

  List<IBranchLayoutEntry> getSubbranches();

  Optional<String> getCustomAnnotation();
}
