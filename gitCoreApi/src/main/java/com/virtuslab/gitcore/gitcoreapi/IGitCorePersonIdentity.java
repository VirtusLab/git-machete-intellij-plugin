package com.virtuslab.gitcore.gitcoreapi;

public interface IGitCorePersonIdentity {
  String getName() throws GitException;

  String getEmail() throws GitException;
}
