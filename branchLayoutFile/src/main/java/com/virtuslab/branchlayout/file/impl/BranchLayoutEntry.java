package com.virtuslab.branchlayout.file.impl;

import lombok.Data;

import io.vavr.collection.List;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

@Data
public class BranchLayoutEntry implements IBranchLayoutEntry {
  private final String name;
  private final String customAnnotation;
  private final List<IBranchLayoutEntry> subbranches;

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }
}
