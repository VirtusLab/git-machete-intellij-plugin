package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;
import java.util.Date;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.powermock.api.mockito.PowerMockito;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.BaseGitCoreCommitHash;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCorePersonIdentity;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;

public class TestUtils {

  static TestGitCoreCommit getCommit(BaseGitCoreCommit parentCommit) {
    assert parentCommit == null || parentCommit instanceof TestGitCoreCommit;
    return new TestGitCoreCommit((TestGitCoreCommit) parentCommit);
  }

  static IGitCoreLocalBranch getGitCoreLocalBranch(BaseGitCoreCommit pointedCommit)
      throws GitCoreException {
    return getGitCoreLocalBranch(pointedCommit, null, false);
  }

  static IGitCoreLocalBranch getGitCoreLocalBranch(BaseGitCoreCommit pointedCommit, BaseGitCoreCommit forkPoint)
      throws GitCoreException {
    return getGitCoreLocalBranch(pointedCommit, forkPoint, false);
  }

  static IGitCoreLocalBranch getGitCoreLocalBranch(BaseGitCoreCommit pointedCommit, BaseGitCoreCommit forkPoint,
      boolean hasJustBeenCreated)
      throws GitCoreException {
    IGitCoreLocalBranch mock = PowerMockito.mock(IGitCoreLocalBranch.class);
    PowerMockito.doReturn(pointedCommit).when(mock).getPointedCommit();
    PowerMockito.doReturn(Option.of(forkPoint)).when(mock).deriveForkPoint();
    PowerMockito.doReturn(hasJustBeenCreated).when(mock).hasJustBeenCreated();
    return mock;
  }

  static class TestGitCoreCommit extends BaseGitCoreCommit {
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
    public Date getCommitTime() {
      return null;
    }

    @Override
    public BaseGitCoreCommitHash getHash() {
      return new BaseGitCoreCommitHash() {
        @Override
        public String getHashString() {
          return String.valueOf(id);
        }
      };
    }
  }

  static class TestGitCoreRepository implements IGitCoreRepository {
    @Override
    public boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant) {
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
    public IGitCoreLocalBranch getLocalBranch(String branchName) {
      return null;
    }

    @Override
    public IGitCoreRemoteBranch getRemoteBranch(String branchName) {
      return null;
    }

    @Override
    public List<IGitCoreLocalBranch> getLocalBranches() {
      return List.empty();
    }

    @Override
    public List<IGitCoreRemoteBranch> getRemoteBranches() {
      return List.empty();
    }

    @Override
    public Path getMainDirectoryPath() {
      return null;
    }

    @Override
    public Path getGitDirectoryPath() {
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
