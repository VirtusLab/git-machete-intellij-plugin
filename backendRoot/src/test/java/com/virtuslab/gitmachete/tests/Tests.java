package com.virtuslab.gitmachete.tests;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.backendroot.GitFactoryModule;
import com.virtuslab.gitmachete.gitmacheteapi.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;

public class Tests {
  IGitMacheteRepository repo;

  @Test
  public void Test() throws GitMacheteException, IOException, URISyntaxException {
    // Prepare repo

    Path tmp = Paths.get("/var/tmp/machete-tests");
    Path scripts = tmp.resolve("scripts");
    Path repo1Script = scripts.resolve("repo1.sh");
    Path repo1 = tmp.resolve("machete-sandbox");

    Files.createDirectory(tmp);
    Files.createDirectory(scripts);

    Files.copy(Paths.get(getClass().getResource("/repo1.sh").toURI()), repo1Script);

    Runtime.getRuntime()
        .exec(
            "/bin/bash "
                + repo1Script.toAbsolutePath().toString()
                + " "
                + tmp.toAbsolutePath().toString());

    repo =
        GitFactoryModule.getInjector()
            .getInstance(GitMacheteRepositoryFactory.class)
            .create(repo1, Optional.empty());


    //Test

    String myResult = repoStatusLikeCli();

    var gitMacheteProcessBuilder = new ProcessBuilder();
    gitMacheteProcessBuilder.command("git", "machete", "status", "-l");
    gitMacheteProcessBuilder.directory(repo1.toFile());
    var gitMacheteProcess = gitMacheteProcessBuilder.start();
    String gitMacheteResult = convertStreamToString(gitMacheteProcess.getInputStream());

    Assert.assertEquals(gitMacheteResult, myResult);

    //Cleanup
      Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
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
