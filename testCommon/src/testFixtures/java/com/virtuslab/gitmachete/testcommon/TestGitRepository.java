package com.virtuslab.gitmachete.testcommon;

import static com.virtuslab.gitmachete.testcommon.TestFileUtils.copyScriptFromResources;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.prepareRepoFromScript;
import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;

import java.nio.file.Files;
import java.nio.file.Path;

import lombok.SneakyThrows;

public class TestGitRepository {

  // When set, TestGitRepository copies `<prebuiltTemplatesDir>/<scriptName>/` into the per-test
  // temp dir instead of re-running the setup script. The :testCommon:prepareTestRepoTemplates
  // Gradle task populates this directory once per build and wires the property onto every
  // dependent Test task; running tests from inside the IDE without that task still works -
  // we fall back to executing the script in-process.
  private static final String PREBUILT_TEMPLATES_DIR_PROPERTY = "testFixtures.prebuiltTemplatesDir";

  private static final String SETUP_WITH_SINGLE_REMOTE = "setup-with-single-remote.sh";

  public final Path parentDirectoryPath;
  public final Path rootDirectoryPath;
  public final Path mainGitDirectoryPath;
  public final Path worktreeGitDirectoryPath;

  @SneakyThrows
  public TestGitRepository(String scriptName) {
    parentDirectoryPath = Files.createTempDirectory("machete-tests-");
    if (scriptName.equals(SETUP_WITH_SINGLE_REMOTE)) {
      rootDirectoryPath = parentDirectoryPath.resolve("machete-sandbox-worktree");
      mainGitDirectoryPath = parentDirectoryPath.resolve("machete-sandbox").resolve(".git");
      worktreeGitDirectoryPath = mainGitDirectoryPath.resolve("worktrees").resolve("machete-sandbox-worktree");
    } else {
      rootDirectoryPath = parentDirectoryPath.resolve("machete-sandbox");
      mainGitDirectoryPath = rootDirectoryPath.resolve(".git");
      worktreeGitDirectoryPath = rootDirectoryPath.resolve(".git");
    }

    materializeBaseRepo(scriptName);

    if (scriptName.equals(SETUP_WITH_SINGLE_REMOTE)) {
      addWorktreeAndDetachMain();
    }

    System.out.println("Set up a test repo in " + rootDirectoryPath);
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

  // Mirrors the tail that setup-with-single-remote.sh used to do inline. Kept in Java so that the
  // absolute paths baked into `.git/worktrees/<wt>/gitdir` are stamped against the per-test
  // parent dir, not against the build-time prebuild dir.
  private void addWorktreeAndDetachMain() {
    final String git = "git";
    Path mainRepo = parentDirectoryPath.resolve("machete-sandbox");
    // Pin the worktree to HEAD (a commit, not a branch) so that git doesn't auto-create a
    // `machete-sandbox-worktree` branch.
    runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 30, git, "worktree", "add", "../machete-sandbox-worktree", "HEAD");
    // Stash the worktree's per-repo hooks override before detaching HEAD on the main repo,
    // matching the original script's ordering.
    runProcessAndReturnStdout(rootDirectoryPath, /* timeoutSeconds */ 10,
        git, "config", "--local", "core.hooksPath", "../machete-sandbox/.git/hooks");
    // git refuses to check out a branch already held by another worktree, so flip the main repo
    // into detached HEAD to give the worktree free rein over every branch.
    String headSha = runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 10, git, "rev-parse", "HEAD").trim();
    runProcessAndReturnStdout(mainRepo, /* timeoutSeconds */ 10, git, "checkout", headSha);
  }
}
