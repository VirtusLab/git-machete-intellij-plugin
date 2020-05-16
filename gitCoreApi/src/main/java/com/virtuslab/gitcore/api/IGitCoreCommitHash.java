package com.virtuslab.gitcore.api;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of
 * {@code getHashString}
 */
public interface IGitCoreCommitHash {
  String getHashString();
}
