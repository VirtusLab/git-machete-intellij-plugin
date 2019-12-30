package com.virtuslab.gitmachete.tests;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.backendroot.GitFactoryModule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import org.junit.Assert;
import org.junit.Test;

public class Tests {
  IGitMacheteRepository repo;

  private static class TestPaths {
    public static final Path tmp = Paths.get("/tmp/machete-tests");
    public static final Path scripts = tmp.resolve("scripts");
    public static final Path repoScript = scripts.resolve("repo.sh");
    public static final Path repo = tmp.resolve("machete-sandbox");
  }

  @Test
  public void StatusTest() throws GitMacheteException, IOException, URISyntaxException {
    // Prepare repo
    createDirStructure();
    copyScriptFromResources("repo1.sh");
    prepareRepoFromScript();

    repo =
        GitFactoryModule.getInjector()
            .getInstance(GitMacheteRepositoryFactory.class)
            .create(TestPaths.repo, Optional.empty());

    // Test
    String myResult = repoStatusLikeCli();
    String gitMacheteResult = gitMacheteCliStatusResult();

    Assert.assertEquals(gitMacheteResult, myResult);

    // Cleanup
    cleanup();
  }

  private void createDirStructure() throws IOException {
    Files.createDirectory(TestPaths.tmp);
    Files.createDirectory(TestPaths.scripts);
  }

  private void copyScriptFromResources(String scriptName) throws URISyntaxException, IOException {
    Files.copy(Paths.get(getClass().getResource("/" + scriptName).toURI()), TestPaths.repoScript);
  }

  private void prepareRepoFromScript() throws IOException {
    Runtime.getRuntime()
        .exec(
            "/bin/bash "
                + TestPaths.repoScript.toAbsolutePath().toString()
                + " "
                + TestPaths.tmp.toAbsolutePath().toString());
  }

  private String gitMacheteCliStatusResult() throws IOException {
    var gitMacheteProcessBuilder = new ProcessBuilder();
    gitMacheteProcessBuilder.command("git", "machete", "status", "-l");
    gitMacheteProcessBuilder.directory(TestPaths.repo.toFile());
    var gitMacheteProcess = gitMacheteProcessBuilder.start();
    return convertStreamToString(gitMacheteProcess.getInputStream());
  }

  private void cleanup() throws IOException {
    Files.walk(TestPaths.tmp)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  private String repoStatusLikeCli() {
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

        var commits = branch.getCommits();
        Collections.reverse(commits);

        for (var c : commits) {
          sb.append("  ");
          sb.append("| ".repeat(level));
          sb.append(c.getMessage().split("\n", 2)[0]);
          sb.append(System.lineSeparator());
        }

        sb.append("  ");
        sb.append("| ".repeat(level - 1));
        var parentStatus = branch.getSyncToParentStatus();
        if (parentStatus == SyncToParentStatus.InSync) sb.append("o");
        else if (parentStatus == SyncToParentStatus.OutOfSync) sb.append("x");
        else if (parentStatus == SyncToParentStatus.NotADirectDescendant) sb.append("?");
        else if (parentStatus == SyncToParentStatus.Merged) sb.append("m");
        sb.append("-");
      }

      sb.append(branch.getName());

      var currBranch = repo.getCurrentBranch();
      if (currBranch.isPresent() && currBranch.get().equals(branch)) sb.append(" *");

      if (branch.getCustomAnnotation().isPresent()) {
        sb.append("  ");
        sb.append(branch.getCustomAnnotation().get());
      }
      var originSync = branch.getSyncToOriginStatus();
      if (originSync != SyncToOriginStatus.InSync) {
        sb.append(" (");
        if (originSync == SyncToOriginStatus.Ahead) sb.append("ahead of origin");
        if (originSync == SyncToOriginStatus.Behind) sb.append("behind origin");
        if (originSync == SyncToOriginStatus.Untracked) sb.append("untracked");
        if (originSync == SyncToOriginStatus.Diverged) sb.append("diverged from origin");
        sb.append(")");
      }
      sb.append(System.lineSeparator());
    } catch (GitException | GitMacheteException e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
    }

    for (var b : branch.getBranches()) {
      printBranch(b, level + 1, sb);
    }
  }

  private static String convertStreamToString(java.io.InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
