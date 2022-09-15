package com.virtuslab.branchlayout.api;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class implementing this interface is reference equality
 */
public interface IBranchLayoutEntry {
  String getName();

  List<IBranchLayoutEntry> getChildren();

  IBranchLayoutEntry withChildren(List<IBranchLayoutEntry> newChildren);

  @Nullable
  String getCustomAnnotation();
}
