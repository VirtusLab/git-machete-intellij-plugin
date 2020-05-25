package com.virtuslab.gitcore.api;

import io.vavr.collection.List;

public interface IGitCoreBranch {
  boolean isLocal();

  String getShortName();

  String getFullName();

  IGitCoreCommit derivePointedCommit() throws GitCoreException;

  List<IGitCoreReflogEntry> deriveReflog() throws GitCoreException;
}
