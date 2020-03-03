package com.virtuslab.gitcore.api;

public interface IGitCorePersonIdentity {
  String getName() throws GitCoreException;

  String getEmail() throws GitCoreException;
}
