package com.virtuslab.gitcore.impl.jgit;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_NO_REMOTES;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import lombok.SneakyThrows;
import lombok.val;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
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
   * GitCoreRepository scoped at the linked worktree's root; JGit resolves the per-worktree git dir
   * ({@code <main-repo>/.git/worktrees/<wt>}) by following the gitlink at {@code <wt-root>/.git}.
   * This is the typical layout when the IDE is opened on a linked worktree.
   */
  @SneakyThrows
  private GitCoreRepository worktreeScoped() {
    return new GitCoreRepository(repo.rootDirectoryPath);
  }

  /**
   * GitCoreRepository scoped at the main repo's root (worktree git dir == main git dir;
   * same shape as a non-worktree project).
   */
  @SneakyThrows
  private GitCoreRepository mainScoped() {
    return new GitCoreRepository(repo.mainGitDirectoryPath.getParent());
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

  // Pins down the per-path-getter contract that downstream callers (machete-file location,
  // merge-base cache, snapshot equality checks in EnhancedGraphTable) rely on.

  @Test
  @SneakyThrows
  public void getters_onLinkedWorktree_resolveToDistinctPaths() {
    val gitCoreRepository = worktreeScoped();

    // Root is the linked worktree's checked-out directory, not the main repo's root.
    assertEquals(repo.rootDirectoryPath.toRealPath(), gitCoreRepository.getRootDirectoryPath().toRealPath());

    // The per-worktree git dir is `<main>/.git/worktrees/<wt>`, distinct from the common dir.
    assertEquals(repo.worktreeGitDirectoryPath.toRealPath(), gitCoreRepository.getWorktreeGitDirectoryPath().toRealPath());

    // The common (main) git dir is reachable via JGit's `commondir` pointer support.
    assertEquals(repo.mainGitDirectoryPath.toRealPath(), gitCoreRepository.getMainGitDirectoryPath().toRealPath());

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void getters_onPlainSingleRepo_haveMainEqualToWorktreeGitDir() {
    // Tear down the SETUP_WITH_SINGLE_REMOTE fixture and set up a non-worktree one for this case.
    cleanUpDir(repo.parentDirectoryPath);
    repo = new TestGitRepository(SETUP_FOR_NO_REMOTES);

    val gitCoreRepository = new GitCoreRepository(repo.rootDirectoryPath);

    // For a plain single-worktree repo there's no `commondir` pointer file, so the per-worktree
    // git dir IS the common git dir. Both getters must agree, and both must equal `<root>/.git`.
    val rootDotGit = repo.rootDirectoryPath.resolve(".git").toRealPath();
    assertEquals(rootDotGit, gitCoreRepository.getMainGitDirectoryPath().toRealPath());
    assertEquals(rootDotGit, gitCoreRepository.getWorktreeGitDirectoryPath().toRealPath());
    assertEquals(repo.rootDirectoryPath.toRealPath(), gitCoreRepository.getRootDirectoryPath().toRealPath());

    cleanUpDir(repo.parentDirectoryPath);
  }

  @Test
  @SneakyThrows
  public void jgitFindGitDirFromLinkedWorktreeRoot_followsTheGitlinkFile() {
    // Pointing JGit's FileRepositoryBuilder at the linked worktree's root must yield the
    // per-worktree git dir (`<main>/.git/worktrees/<wt>`), not the main `.git` dir, by
    // following the gitlink file at `<wt-root>/.git`.
    val builder = new FileRepositoryBuilder()
        .findGitDir(repo.rootDirectoryPath.toFile())
        .setMustExist(true);
    try (val resolved = builder.build()) {
      assertEquals(repo.worktreeGitDirectoryPath.toRealPath(), resolved.getDirectory().toPath().toRealPath());
      // Common-dir support landed in JGit 7.0; without it the refactor doesn't fly.
      assertEquals(repo.mainGitDirectoryPath.toRealPath(), resolved.getCommonDirectory().toPath().toRealPath());
      assertEquals(repo.rootDirectoryPath.toRealPath(), resolved.getWorkTree().toPath().toRealPath());
    }

    cleanUpDir(repo.parentDirectoryPath);
  }
}
