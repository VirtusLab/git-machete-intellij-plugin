package com.virtuslab.gitmachete.backend.unit;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.powermock.api.mockito.PowerMockito;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCorePersonIdentity;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

public class TestUtils {

  static TestGitCoreCommit createGitCoreCommit() {
    return new TestGitCoreCommit();
  }

  @SneakyThrows
  static IGitCoreLocalBranch createGitCoreLocalBranch(IGitCoreCommit pointedCommit, IGitCoreReflogEntry... reflogEntries) {
    IGitCoreLocalBranch mock = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(pointedCommit).when(mock).derivePointedCommit();
    PowerMockito.doReturn(List.ofAll(Stream.of(reflogEntries))).when(mock).deriveReflog();
    PowerMockito.doReturn(Option.none()).when(mock).getRemoteTrackingBranch();
    return mock;
  }

  static class TestGitCoreCommitHash implements IGitCoreCommitHash {

    private static final AtomicInteger counter = new AtomicInteger(0);

    private final int id;

    TestGitCoreCommitHash() {
      id = counter.incrementAndGet();
    }

    @Override
    public String getHashString() {
      return String.valueOf(id);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return IGitCoreCommitHash.defaultEquals(this, other);
    }

    @Override
    public int hashCode() {
      return IGitCoreCommitHash.defaultHashCode(this);
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
    public String getShortMessage() {
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
      return IGitCoreCommit.defaultEquals(this, other);
    }

    @Override
    public final int hashCode() {
      return IGitCoreCommit.defaultHashCode(this);
    }
  }
}
