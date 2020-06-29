package com.virtuslab.branchlayout.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

/**
 * The only criterion for equality of any instances of any class implementing this interface is reference equality
 */
public interface IBranchLayoutEntry {
  String getName();

  List<IBranchLayoutEntry> getSubentries();

  IBranchLayoutEntry withSubentries(List<IBranchLayoutEntry> newSubentry);

  Option<String> getCustomAnnotation();
}
