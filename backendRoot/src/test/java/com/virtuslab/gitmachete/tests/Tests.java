package com.virtuslab.gitmachete.tests;

import com.virtuslab.gitmachete.backendroot.BackendFactoryModule;
import com.virtuslab.gitmachete.backendroot.GitMacheteRepositoryBuilderFactory;
import com.virtuslab.gitmachete.backendroot.IGitMacheteRepositoryBuilder;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Tests {
  IGitMacheteRepository repo;
  private final GitMacheteRepositoryBuilderFactory gitMacheteRepositoryBuilderFactory =
      BackendFactoryModule.getInjector().getInstance(GitMacheteRepositoryBuilderFactory.class);

  private static class TestPaths {
    public static final Path tmp = Paths.get("/tmp/machete-tests");
    public static final Path scripts = tmp.resolve("scripts");
    public static final Path repoScript = scripts.resolve("repo.sh");
    public static final Path repo = tmp.resolve("machete-sandbox");
  }

  @Before
  public void init() throws Exception {
    // Prepare repo
    createDirStructure();
    copyScriptFromResources("repo1.sh");
    prepareRepoFromScript();

    IGitMacheteRepositoryBuilder repoBuilder =
        BackendFactoryModule.getInjector()
            .getInstance(GitMacheteRepositoryBuilderFactory.class)
            .create(TestPaths.repo);

    repo = repoBuilder.build();
  }

  @Test
  public void StatusTest() throws Exception {
    String myResult = repositoryStatus();
    String gitMacheteCliStatus = gitMacheteCliStatus();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println("MY OUTPUT:");
    System.out.println(myResult);

    Assert.assertEquals(gitMacheteCliStatus, myResult);
  }

  private void createDirStructure() throws IOException {
    Files.createDirectory(TestPaths.tmp);
    Files.createDirectory(TestPaths.scripts);
  }

  private void copyScriptFromResources(String scriptName) throws URISyntaxException, IOException {
    Files.copy(Paths.get(getClass().getResource("/" + scriptName).toURI()), TestPaths.repoScript);
  }

  private void prepareRepoFromScript() throws IOException, InterruptedException {
    var r =
        Runtime.getRuntime()
            .exec(
                "/bin/bash "
                    + TestPaths.repoScript.toAbsolutePath().toString()
                    + " "
                    + TestPaths.tmp.toAbsolutePath().toString());
    r.waitFor(1, TimeUnit.SECONDS);
  }

  private String gitMacheteCliStatus() throws IOException {
    var gitMacheteProcessBuilder = new ProcessBuilder();
    gitMacheteProcessBuilder.command("git", "machete", "status", "-l");
    gitMacheteProcessBuilder.directory(TestPaths.repo.toFile());
    var gitMacheteProcess = gitMacheteProcessBuilder.start();
    return convertStreamToString(gitMacheteProcess.getInputStream());
  }

  @After
  public void cleanup() throws IOException {
    Files.walk(TestPaths.tmp)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  private String repositoryStatus() {
    var sb = new StringBuilder();
    int lastRootBranch = repo.getRootBranches().size() - 1;
    var branches = repo.getRootBranches();
    for (int currentRootBranch = 0; currentRootBranch <= lastRootBranch; currentRootBranch++) {
      var b = branches.get(currentRootBranch);
      printBranch(b, 0, sb);
      if (currentRootBranch < lastRootBranch) sb.append(System.lineSeparator());
    }

    return sb.toString();
  }

  private void printBranch(IGitMacheteBranch branch, int level, StringBuilder sb) {
    try {
      sb.append("  ");

      if (level > 0) {
        sb.append("| ".repeat(level));

        sb.append(System.lineSeparator());

        var commits = branch.computeCommits();
        Collections.reverse(commits);

        for (var c : commits) {
          sb.append("  ");
          sb.append("| ".repeat(level));
          sb.append(c.getMessage().split("\n", 2)[0]);
          sb.append(System.lineSeparator());
        }

        sb.append("  ");
        sb.append("| ".repeat(level - 1));
        var parentStatus = branch.computeSyncToParentStatus();
        if (parentStatus == SyncToParentStatus.InSync) sb.append("o");
        else if (parentStatus == SyncToParentStatus.OutOfSync) sb.append("x");
        else if (parentStatus == SyncToParentStatus.InSyncButForkPointOff) sb.append("?");
        else if (parentStatus == SyncToParentStatus.Merged) sb.append("m");
        sb.append("-");
      }

      sb.append(branch.getName());

      var currBranch = repo.getCurrentBranchIfManaged();
      if (currBranch.isPresent() && currBranch.get().equals(branch)) sb.append(" *");

      if (branch.getCustomAnnotation().isPresent()) {
        sb.append("  ");
        sb.append(branch.getCustomAnnotation().get());
      }
      var originSync = branch.computeSyncToOriginStatus();
      if (originSync != SyncToOriginStatus.InSync) {
        sb.append(" (");
        if (originSync == SyncToOriginStatus.Ahead) sb.append("ahead of origin");
        if (originSync == SyncToOriginStatus.Behind) sb.append("behind origin");
        if (originSync == SyncToOriginStatus.Untracked) sb.append("untracked");
        if (originSync == SyncToOriginStatus.Diverged) sb.append("diverged from origin");
        sb.append(")");
      }
      sb.append(System.lineSeparator());
    } catch (GitMacheteException e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
    }

    for (var b : branch.getDownstreamBranches()) {
      printBranch(b, level + 1, sb);
    }
  }

  private static String convertStreamToString(InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
