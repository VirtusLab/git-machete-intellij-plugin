package com.virtuslab.gitmachete.backend.unit;

import java.nio.file.Path;
import java.time.Instant;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.powermock.api.mockito.PowerMockito;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCorePersonIdentity;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;

public class TestUtils {

  static TestGitCoreCommit getCommit(IGitCoreCommit parentCommit) {
    assert parentCommit == null || parentCommit instanceof TestGitCoreCommit;
    return new TestGitCoreCommit((TestGitCoreCommit) parentCommit);
  }

  static IGitCoreLocalBranch getGitCoreLocalBranch(IGitCoreCommit pointedCommit)
      throws GitCoreException {
    return getGitCoreLocalBranch(pointedCommit, null, false);
  }

  static IGitCoreLocalBranch getGitCoreLocalBranch(IGitCoreCommit pointedCommit, IGitCoreCommit forkPoint)
      throws GitCoreException {
    return getGitCoreLocalBranch(pointedCommit, forkPoint, false);
  }

  static IGitCoreLocalBranch getGitCoreLocalBranch(IGitCoreCommit pointedCommit, IGitCoreCommit forkPoint,
      boolean hasJustBeenCreated)
      throws GitCoreException {
    IGitCoreLocalBranch mock = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(pointedCommit).when(mock).getPointedCommit();
    PowerMockito.doReturn(Option.of(forkPoint)).when(mock).deriveForkPoint();
    PowerMockito.doReturn(hasJustBeenCreated).when(mock).hasJustBeenCreated();
    return mock;
  }

  static class TestGitCoreCommit implements IGitCoreCommit {
    private static int counter;

    private final int id;

    private final TestGitCoreCommit parentCommit;

    TestGitCoreCommit(TestGitCoreCommit parentCommit) {
      this.parentCommit = parentCommit;
      id = counter++;
    }

    TestGitCoreCommit getParentCommit() {
      return parentCommit;
    }

    @Override
    public String getMessage() {
      return null;
    }

    @Override
    public IGitCorePersonIdentity getAuthor() {
      return null;
    }

    @Override
    public IGitCorePersonIdentity getCommitter() {
      return null;
    }

    @Override
    public Instant getCommitTime() {
      return null;
    }

    @Override
    public IGitCoreCommitHash getHash() {
      return new IGitCoreCommitHash() {
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
      };
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

  static class TestGitCoreRepository implements IGitCoreRepository {
    @Override
    public boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) {
      assert presumedAncestor instanceof TestGitCoreCommit;
      assert presumedDescendant instanceof TestGitCoreCommit;
      while (presumedDescendant != null && !presumedDescendant.equals(presumedAncestor)) {
        presumedDescendant = ((TestGitCoreCommit) presumedDescendant).getParentCommit();
      }
      return presumedDescendant != null;
    }

    @Override
    public Option<IGitCoreLocalBranch> getCurrentBranch() {
      return Option.none();
    }

    @Override
    public IGitCoreLocalBranch getLocalBranch(String localBranchShortName) {
      return null;
    }

    @Override
    public List<IGitCoreLocalBranch> getLocalBranches() {
      return List.empty();
    }

    @Override
    public List<IGitCoreRemoteBranch> getRemoteBranches(String remoteName) {
      return List.empty();
    }

    @Override
    public List<IGitCoreRemoteBranch> getAllRemoteBranches() {
      return List.empty();
    }

    @Override
    public List<String> getRemotes() {
      return List.empty();
    }

    @Override
    public Path getMainDirectoryPath() {
      return null;
    }
  }

  static class TestGitCoreRepositoryFactory implements IGitCoreRepositoryFactory {

    private final TestGitCoreRepository instance;

    public TestGitCoreRepositoryFactory() {
      instance = new TestGitCoreRepository();
    }

    public TestGitCoreRepository getInstance() {
      return instance;
    }

    @Override
    public IGitCoreRepository create(Path mainDirectoryPath, Path gitDirectoryPath) {
      return getInstance();
    }
  }
}
