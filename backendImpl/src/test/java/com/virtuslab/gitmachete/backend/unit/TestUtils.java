package com.virtuslab.gitmachete.backend.unit;

import java.time.Instant;
import java.util.Arrays;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.powermock.api.mockito.PowerMockito;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCorePersonIdentity;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

public class TestUtils {

  static TestGitCoreCommit createGitCoreCommit() {
    return new TestGitCoreCommit();
  }

  static IGitCoreLocalBranch createGitCoreLocalBranch(IGitCoreCommit pointedCommit, IGitCoreReflogEntry... reflogEntries)
      throws GitCoreException {
    IGitCoreLocalBranch mock = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(pointedCommit).when(mock).derivePointedCommit();
    PowerMockito.doReturn(List.ofAll(Arrays.asList(reflogEntries))).when(mock).deriveReflog();
    return mock;
  }

  static class TestGitCoreCommitHash implements IGitCoreCommitHash {

    private static int counter;

    private final int id;

    TestGitCoreCommitHash() {
      id = counter++;
    }

    @Override
    public String getHashString() {
      return String.valueOf(id);
    }

    @Override
    public final boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      } else if (!(other instanceof IGitCoreCommitHash)) {
        return false;
      } else {
        return getHashString().equals(((IGitCoreCommitHash) other).getHashString());
      }
    }

    @Override
    public final int hashCode() {
      return getHashString().hashCode();
    }
  }

  static class TestGitCoreReflogEntry implements IGitCoreReflogEntry {
    @Override
    public String getComment() {
      return "Lorem ipsum";
    }

    @Override
    public Option<IGitCoreCommitHash> getOldCommitHash() {
      return Option.some(new TestGitCoreCommitHash());
    }

    @Override
    public IGitCoreCommitHash getNewCommitHash() {
      return new TestGitCoreCommitHash();
    }
  }

  static class TestGitCoreCommit implements IGitCoreCommit {
    @Override
    public String getMessage() {
      throw new NotImplementedError();
    }

    @Override
    public IGitCorePersonIdentity getAuthor() {
      throw new NotImplementedError();
    }

    @Override
    public IGitCorePersonIdentity getCommitter() {
      throw new NotImplementedError();
    }

    @Override
    public Instant getCommitTime() {
      throw new NotImplementedError();
    }

    @Override
    public IGitCoreCommitHash getHash() {
      return new TestGitCoreCommitHash();
    }

    @Override
    public final boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      } else if (!(other instanceof IGitCoreCommit)) {
        return false;
      } else {
        return getHash().equals(((IGitCoreCommit) other).getHash());
      }
    }

    @Override
    public final int hashCode() {
      return getHash().hashCode();
    }
  }
}
