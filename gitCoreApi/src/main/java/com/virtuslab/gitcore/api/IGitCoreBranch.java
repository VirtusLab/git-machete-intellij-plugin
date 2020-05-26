package com.virtuslab.gitcore.api;

import io.vavr.collection.List;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of
 * {@code derivePointedCommit} and {@code getFullName}
 */
public interface IGitCoreBranch {
  boolean isLocal();

  String getShortName();

  String getFullName();

  IGitCoreCommit derivePointedCommit() throws GitCoreException;

  List<IGitCoreReflogEntry> deriveReflog() throws GitCoreException;
}
