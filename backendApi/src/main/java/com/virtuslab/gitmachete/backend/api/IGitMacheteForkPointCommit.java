package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;

public interface IGitMacheteForkPointCommit extends IGitMacheteCommit {
  List<String> getBranchesContainingInReflog();

  boolean isOverridden();
}
