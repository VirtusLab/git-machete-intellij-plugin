package com.virtuslab.gitmachete.frontend.actions.common;

import java.util.Collections;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsCommitMetadata;
import com.intellij.vcs.log.VcsUser;
import com.intellij.vcs.log.impl.HashImpl;
import io.vavr.NotImplementedError;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;

public class VcsCommitMetadataAdapter implements VcsCommitMetadata {

  private final java.util.List<Hash> parents;
  private final Hash hash;
  private final String fullMessage;

  public VcsCommitMetadataAdapter(ICommitOfManagedBranch parent, ICommitOfManagedBranch commit) {
    this.parents = Collections.singletonList(HashImpl.build(parent.getHash()));
    this.hash = HashImpl.build(commit.getHash());
    this.fullMessage = commit.getFullMessage();
  }

  @Override
  public String getFullMessage() {
    return fullMessage;
  }

  @Override
  public VirtualFile getRoot() {
    throw new NotImplementedError();
  }

  @Override
  public String getSubject() {
    throw new NotImplementedError();
  }

  @Override
  public VcsUser getAuthor() {
    throw new NotImplementedError();
  }

  @Override
  public VcsUser getCommitter() {
    throw new NotImplementedError();
  }

  @Override
  public long getAuthorTime() {
    throw new NotImplementedError();
  }

  @Override
  public long getCommitTime() {
    throw new NotImplementedError();
  }

  @Override
  public Hash getId() {
    return hash;
  }

  /**
   * Hackish approach to use {@link git4idea.rebase.log.squash.GitSquashOperation}.
   * Underneath, in {@link git4idea.rebase.log.GitCommitEditingOperation}
   * it calls {@code val base = commits.last().parents.first().asString()} to retrieve the base.
   */
  @Override
  public java.util.List<Hash> getParents() {
    return parents;
  }

  @Override
  public long getTimestamp() {
    throw new NotImplementedError();
  }
}
