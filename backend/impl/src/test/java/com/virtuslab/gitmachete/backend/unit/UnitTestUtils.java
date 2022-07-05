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

import com.virtuslab.gitcore.api.IGitCoreCheckoutEntry;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreObjectHash;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreTreeHash;

class UnitTestUtils {

  private static final AtomicInteger counter = new AtomicInteger(0);

  static TestGitCoreCommit createGitCoreCommit() {
    return new TestGitCoreCommit();
  }

  @SneakyThrows
  static IGitCoreLocalBranchSnapshot createGitCoreLocalBranch(IGitCoreCommit pointedCommit,
      IGitCoreReflogEntry... reflogEntries) {
    IGitCoreLocalBranchSnapshot mock = PowerMockito.mock(IGitCoreLocalBranchSnapshot.class);
    PowerMockito.doReturn(String.valueOf(counter.incrementAndGet())).when(mock).getFullName();
    PowerMockito.doReturn(pointedCommit).when(mock).getPointedCommit();
    PowerMockito.doReturn(List.ofAll(Stream.of(reflogEntries))).when(mock).getReflogFromMostRecent();
    PowerMockito.doReturn(Option.none()).when(mock).getRemoteTrackingBranch();
    return mock;
  }

  static class TestGitCoreCommitHash implements IGitCoreCommitHash {

    private final int id;

    TestGitCoreCommitHash() {
      id = counter.incrementAndGet();
    }

    @Override
    public String getHashString() {
      return String.format("%40d", id);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return IGitCoreObjectHash.defaultEquals(this, other);
    }

    @Override
    public int hashCode() {
      return IGitCoreObjectHash.defaultHashCode(this);
    }
  }

  static class TestGitCoreReflogEntry implements IGitCoreReflogEntry {
    @Override
    public String getComment() {
      return "Lorem ipsum";
    }

    @Override
    public Instant getTimestamp() {
      return Instant.now();
    }

    @Override
    public Option<IGitCoreCommitHash> getOldCommitHash() {
      return Option.some(new TestGitCoreCommitHash());
    }

    @Override
    public IGitCoreCommitHash getNewCommitHash() {
      return new TestGitCoreCommitHash();
    }

    @Override
    public Option<IGitCoreCheckoutEntry> parseCheckout() {
      return Option.none();
    }
  }

  static class TestGitCoreCommit implements IGitCoreCommit {
    @Override
    public String getShortMessage() {
      return "test commit message";
    }

    @Override
    public String getFullMessage() {
      return getShortMessage();
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
    public IGitCoreTreeHash getTreeHash() {
      throw new NotImplementedError();
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
