package com.virtuslab.gitmachete.testcommon;

import static com.virtuslab.gitmachete.testcommon.TestFileUtils.copyScriptFromResources;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.prepareRepoFromScript;
import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;

import java.nio.file.Files;
import java.nio.file.Path;

import lombok.SneakyThrows;

// `"git"` and `".git"` repeat naturally in subprocess-driven Git fixture code; extracting them
// into constants would only obscure the call sites without any readability gain.
@SuppressWarnings("MultipleStringLiterals")
public class TestGitRepository {

  // When set, TestGitRepository copies `<prebuiltTemplatesDir>/<scriptName>/` into the per-test
  // temp dir instead of re-running the setup script. The :testCommon:prepareTestRepoTemplates
  // Gradle task populates this directory once per build and wires the property onto every
  // dependent Test task; running tests from inside the IDE without that task still works -
  // we fall back to executing the script in-process.
  private static final String PREBUILT_TEMPLATES_DIR_PROPERTY = "testFixtures.prebuiltTemplatesDir";

  private static final String SETUP_WITH_SINGLE_REMOTE = "setup-with-single-remote.sh";
  private static final String SETUP_WITH_WORKTREES = "setup-with-worktrees.sh";

  private static final String MAIN_REPO_DIR_NAME = "machete-sandbox";
  private static final String WORKTREE_A_DIR_NAME = "worktree-a";
  private static final String WORKTREE_B_DIR_NAME = "worktree-b";

  public final Path parentDirectoryPath;
  public final Path rootDirectoryPath;
  public final Path mainGitDirectoryPath;
  public final Path worktreeGitDirectoryPath;

  @SneakyThrows
  public TestGitRepository(String scriptName) {
    parentDirectoryPath = Files.createTempDirectory("machete-tests-");
    if (scriptName.equals(SETUP_WITH_SINGLE_REMOTE)) {
      rootDirectoryPath = parentDirectoryPath.resolve("machete-sandbox-worktree");
      mainGitDirectoryPath = parentDirectoryPath.resolve(MAIN_REPO_DIR_NAME).resolve(".git");
      worktreeGitDirectoryPath = mainGitDirectoryPath.resolve("worktrees").resolve("machete-sandbox-worktree");
    } else if (scriptName.equals(SETUP_WITH_WORKTREES)) {
      // Drive both the snapshot and the CLI from a linked worktree (not the main repo), so the
      // status output covers all three worktree-label variants in one shot: `<this worktree>`
      // (the linked one we're rooted at), `<main worktree>` (the main repo holding `master`),
      // and an explicitly-named linked worktree (the other one, holding `feature-b`).
      rootDirectoryPath = parentDirectoryPath.resolve(WORKTREE_A_DIR_NAME);
      mainGitDirectoryPath = parentDirectoryPath.resolve(MAIN_REPO_DIR_NAME).resolve(".git");
      worktreeGitDirectoryPath = mainGitDirectoryPath.resolve("worktrees").resolve(WORKTREE_A_DIR_NAME);
    } else {
      rootDirectoryPath = parentDirectoryPath.resolve(MAIN_REPO_DIR_NAME);
      mainGitDirectoryPath = rootDirectoryPath.resolve(".git");
      worktreeGitDirectoryPath = rootDirectoryPath.resolve(".git");
    }

    materializeBaseRepo(scriptName);
    applyPostScriptSetup(scriptName, parentDirectoryPath);

    System.out.println("Set up a test repo in " + rootDirectoryPath);
  }

  /**
   * Working directory from which to invoke {@code git machete} so its output matches the snapshot
   * built by {@link #TestGitRepository(String)}. Kept in sync with the {@code rootDirectoryPath}
   * branches above; {@link com.virtuslab.gitmachete.backend.integration.RegenerateCliOutputs}
   * dispatches here so the regenerated CLI fixtures don't drift from what the tests assert.
   */
  public static Path cliWorkingDirectory(String scriptName, Path parentDirectoryPath) {
    if (scriptName.equals(SETUP_WITH_WORKTREES)) {
      return parentDirectoryPath.resolve(WORKTREE_A_DIR_NAME);
    }
    return parentDirectoryPath.resolve(MAIN_REPO_DIR_NAME);
  }

  @SneakyThrows
  private void materializeBaseRepo(String scriptName) {
    Path prebuiltTarball = locatePrebuiltTarball(scriptName);
    if (prebuiltTarball != null) {
      // ~50 ms vs ~3 s for re-running the script.
      runProcessAndReturnStdout(parentDirectoryPath, /* timeoutSeconds */ 30, "tar", "-xf", prebuiltTarball.toString());
      return;
    }
    copyScriptFromResources("common.sh", parentDirectoryPath);
    copyScriptFromResources(scriptName, parentDirectoryPath);
    prepareRepoFromScript(scriptName, parentDirectoryPath);
  }

  private static Path locatePrebuiltTarball(String scriptName) {
    String dir = System.getProperty(PREBUILT_TEMPLATES_DIR_PROPERTY);
    if (dir == null || dir.isEmpty()) {
      return null;
    }
    Path tarball = Path.of(dir, scriptName + ".tar");
    return Files.isRegularFile(tarball) ? tarball : null;
  }

  /**
   * Mirrors the worktree-add tail that some setup scripts intentionally leave out (see their
   * top-of-file comment). Kept in Java so that the absolute paths baked into
   * {@code .git/worktrees/<wt>/gitdir} get stamped against the actual {@code parentDirectoryPath}
   * - not against the build-time prebuild dir. {@link RegenerateCliOutputs} dispatches here too
   * so the CLI sees the same fixture shape as the tests.
   */
  public static void applyPostScriptSetup(String scriptName, Path parentDirectoryPath) {
    if (scriptName.equals(SETUP_WITH_SINGLE_REMOTE)) {
      addWorktreeAndDetachMain(parentDirectoryPath);
    } else if (scriptName.equals(SETUP_WITH_WORKTREES)) {
      addLinkedWorktreesOnBranches(parentDirectoryPath);
    }
  }

  private static void addWorktreeAndDetachMain(Path parentDirectoryPath) {
    Path mainRepo = parentDirectoryPath.resolve(MAIN_REPO_DIR_NAME);
    Path worktreeRoot = parentDirectoryPath.resolve("machete-sandbox-worktree");
    // Pin the worktree to HEAD (a commit, not a branch) so that git doesn't auto-create a
    // `machete-sandbox-worktree` branch.
    runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 30, "git", "worktree", "add", "../machete-sandbox-worktree",
        "HEAD");
    // Stash the worktree's per-repo hooks override before detaching HEAD on the main repo,
    // matching the original script's ordering.
    runProcessAndReturnStdout(worktreeRoot, /* timeoutSeconds */ 10,
        "git", "config", "--local", "core.hooksPath", "../machete-sandbox/.git/hooks");
    // git refuses to check out a branch already held by another worktree, so flip the main repo
    // into detached HEAD to give the worktree free rein over every branch.
    String headSha = runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 10, "git", "rev-parse", "HEAD").trim();
    runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 10, "git", "checkout", headSha);
  }

  // Adds two linked worktrees, each on a distinct managed branch, so that `git machete status`
  // exercises every worktree-label rendering branch: `<this worktree>` for the branch held by
  // the worktree the snapshot is rooted at (`worktree-a` / `feature-a`), `<main worktree>` for
  // the branch held by the main repo (`master`), and a stripped-prefix linked-worktree name for
  // the remaining linked worktree (`worktree-b` / `feature-b`).
  @SneakyThrows
  private static void addLinkedWorktreesOnBranches(Path parentDirectoryPath) {
    Path mainRepo = parentDirectoryPath.resolve(MAIN_REPO_DIR_NAME);
    runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 30, "git", "worktree", "add", "../" + WORKTREE_A_DIR_NAME,
        "feature-a");
    runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 30, "git", "worktree", "add", "../" + WORKTREE_B_DIR_NAME,
        "feature-b");
    // Skip the `machete-status-branch` hook for this fixture: `common.sh` configures
    // `core.hooksPath=.git/hooks` (a relative path) on the main repo, which silently fails to
    // resolve from inside a linked worktree (where `.git` is a gitlink file, not a directory).
    // Both the CLI and JGit hit this same blind spot, so rather than papering over it with a
    // per-worktree override, drop the hook entirely - this fixture exists to exercise worktree
    // labels, not the status hook.
    Files.deleteIfExists(mainRepo.resolve(".git").resolve("hooks").resolve("machete-status-branch"));
  }
}
