package com.virtuslab.gitcore.impl.jgit;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.virtuslab.gitcore.api.GitCoreRepositoryState;
import com.virtuslab.gitmachete.testcommon.TestGitRepository;

// These tests pin down the worktree-aware contract of GitCoreRepository: which pieces of state
// come from the worktree-specific git dir (HEAD, in-progress ops) vs the common (main) git dir
// (refs, config, objects). They exist to safeguard the refactor that collapses the two internal
// JGit Repository handles into one, relying on JGit 7.0+'s `commondir` support to keep
// per-worktree HEAD/state separate from shared refs/config.
public class GitCoreRepositoryWorktreeIntegrationTest {

  private TestGitRepository repo;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    repo = new TestGitRepository(SETUP_WITH_SINGLE_REMOTE);
  }

  /**
   * GitCoreRepository scoped at the linked worktree's git dir
   * ({@code <repo>/.git/worktrees/<wt>}); the typical layout when the IDE is opened on
   * a linked worktree.
   */
  @SneakyThrows
  private GitCoreRepository worktreeScoped() {
    return new GitCoreRepository(repo.rootDirectoryPath, repo.mainGitDirectoryPath, repo.worktreeGitDirectoryPath);
  }

  /**
   * GitCoreRepository scoped at the main git dir
   * (worktree git dir == main git dir; same shape as a non-worktree project).
   */
  @SneakyThrows
  private GitCoreRepository mainScoped() {
    Path mainRootPath = repo.mainGitDirectoryPath.getParent();
    return new GitCoreRepository(mainRootPath, repo.mainGitDirectoryPath, repo.mainGitDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void head_isReadFromWorktreeGitDirNotMainGitDir() {
    // Park the linked worktree on `drop-constraint` (an existing fixture branch that
    // the main repo is not currently on, so git allows it in this worktree).
    runProcessAndReturnStdout(repo.rootDirectoryPath, /* timeoutSeconds */ 30, "git", "checkout", "drop-constraint");

    val headFromWorktree = worktreeScoped().deriveHead();
    val branchFromWorktree = headFromWorktree.getTargetBranch();
    assertNotNull(branchFromWorktree, "Worktree HEAD must resolve to a branch");
    assertEquals("drop-constraint", branchFromWorktree.getName());

    // The setup script ends with `git checkout $(git rev-parse HEAD)` in the main repo,
    // detaching its HEAD. Main-scoped deriveHead must therefore report a detached HEAD,
    // independently of what's checked out in the linked worktree.
    val headFromMain = mainScoped().deriveHead();
    assertNull(headFromMain.getTargetBranch(),
        "Main repo is in detached HEAD per setup script; worktree's branch must not leak through");

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void localBranches_areAccessibleFromWorktreeGitDir() {
    // Refs live in the common (main) git dir; a worktree-scoped repository must still see them
    // via the `commondir` pointer file inside `.git/worktrees/<wt>`.
    val branchNames = worktreeScoped().deriveAllLocalBranches().map(b -> b.getName()).toJavaSet();
    assertTrue(branchNames.contains("develop"), "develop should be visible from worktree-scoped repo");
    assertTrue(branchNames.contains("master"), "master should be visible from worktree-scoped repo");
    assertTrue(branchNames.contains("hotfix/add-trigger"), "hotfix/add-trigger should be visible from worktree-scoped repo");
    assertTrue(branchNames.contains("drop-constraint"), "drop-constraint should be visible from worktree-scoped repo");

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void config_isAccessibleFromWorktreeGitDir() {
    // common.sh sets `user.email` per-repo; this should be reachable through the worktree's
    // commondir pointer.
    assertEquals("circleci@example.com", worktreeScoped().deriveConfigValue("user", "email"));

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void parseRevision_resolvesBranchRefFromWorktreeGitDir() {
    // parseRevision -> jgit.resolve -> refs are in common dir; must work for worktree-scoped repo.
    assertNotNull(worktreeScoped().parseRevision("refs/heads/develop"));

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void repositoryState_isReadFromWorktreeGitDir_merging() {
    // A sentinel MERGE_HEAD in the worktree's git dir must flip the worktree-scoped repo into MERGING,
    // without affecting the main-scoped repo (which has no MERGE_HEAD of its own).
    val sentinelSha = "0123456789abcdef0123456789abcdef01234567\n";
    Files.writeString(repo.worktreeGitDirectoryPath.resolve("MERGE_HEAD"), sentinelSha, StandardCharsets.UTF_8);

    assertEquals(GitCoreRepositoryState.MERGING, worktreeScoped().deriveRepositoryState());
    assertEquals(GitCoreRepositoryState.NO_OPERATION, mainScoped().deriveRepositoryState());

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void rebasedBranch_isReadFromWorktreeGitDir() {
    // Synthesize an in-progress rebase by dropping a `head-name` file under
    // `.git/worktrees/<wt>/rebase-merge/`. `deriveRebasedBranch` looks for exactly this file.
    val rebaseMergeDir = repo.worktreeGitDirectoryPath.resolve("rebase-merge");
    Files.createDirectories(rebaseMergeDir);
    Files.writeString(rebaseMergeDir.resolve("head-name"), "refs/heads/develop\n", StandardCharsets.UTF_8);

    assertEquals("develop", worktreeScoped().deriveRebasedBranch());
    assertNull(mainScoped().deriveRebasedBranch(), "Main-scoped repo must not see worktree's rebase state");

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void bisectedBranch_isReadFromWorktreeGitDir() {
    Files.writeString(repo.worktreeGitDirectoryPath.resolve("BISECT_START"), "develop\n", StandardCharsets.UTF_8);

    assertEquals("develop", worktreeScoped().deriveBisectedBranch());
    assertNull(mainScoped().deriveBisectedBranch(), "Main-scoped repo must not see worktree's bisect state");

    cleanUpDir(repo.parentDirectoryPath);
  }
}
