package com.virtuslab.gitmachete.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
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

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.root.BackendFactoryModule;
import com.virtuslab.gitmachete.backend.root.GitMacheteRepositoryBuilderFactory;

public class Tests {
  IGitMacheteRepository gitMacheteRepository;
  private final GitMacheteRepositoryBuilderFactory gitMacheteRepositoryBuilderFactory = BackendFactoryModule
      .getInjector().getInstance(GitMacheteRepositoryBuilderFactory.class);

  public static final Path tmpTestDir = Paths.get("/tmp/machete-tests");
  public static final Path scriptsDir = tmpTestDir.resolve("scripts");
  public static final Path repositoryBuildingScript = scriptsDir.resolve("repo.sh");
  public static final Path repositoryDir = tmpTestDir.resolve("machete-sandbox");
  public static final String repositoryPreparingCommand = String.format("/bin/bash %s %s",
      repositoryBuildingScript.toAbsolutePath().toString(), tmpTestDir.toAbsolutePath().toString());

  @Before
  public void init() throws Exception {
    // Prepare repo
    createDirStructure();
    copyScriptFromResources("repo1.sh");
    prepareRepoFromScript();

    gitMacheteRepository = gitMacheteRepositoryBuilderFactory.create(repositoryDir).build();
  }

  @After
  public void cleanup() throws IOException {
    Files.walk(tmpTestDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
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
    Files.createDirectory(tmpTestDir);
    Files.createDirectory(scriptsDir);
  }

  private void copyScriptFromResources(String scriptName) throws URISyntaxException, IOException {
    URL resourceUrl = getClass().getResource("/" + scriptName);
    assert resourceUrl != null : "Can't get resource";
    Files.copy(Paths.get(resourceUrl.toURI()), repositoryBuildingScript);
  }

  private void prepareRepoFromScript() throws IOException, InterruptedException {
    Runtime.getRuntime().exec(repositoryPreparingCommand).waitFor(1, TimeUnit.SECONDS);
  }

  private String gitMacheteCliStatus() throws IOException {
    var gitMacheteProcessBuilder = new ProcessBuilder();
    gitMacheteProcessBuilder.command("git", "machete", "status", "-l");
    gitMacheteProcessBuilder.directory(repositoryDir.toFile());
    var gitMacheteProcess = gitMacheteProcessBuilder.start();
    return convertStreamToString(gitMacheteProcess.getInputStream());
  }

  private String repositoryStatus() {
    var sb = new StringBuilder();
    int lastRootBranch = gitMacheteRepository.getRootBranches().size() - 1;
    var branches = gitMacheteRepository.getRootBranches();
    for (int currentRootBranch = 0; currentRootBranch <= lastRootBranch; currentRootBranch++) {
      var b = branches.get(currentRootBranch);
      printBranch(b, 0, sb);
      if (currentRootBranch < lastRootBranch)
        sb.append(System.lineSeparator());
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
        if (parentStatus == SyncToParentStatus.InSync)
          sb.append("o");
        else if (parentStatus == SyncToParentStatus.OutOfSync)
          sb.append("x");
        else if (parentStatus == SyncToParentStatus.InSyncButForkPointOff)
          sb.append("?");
        else if (parentStatus == SyncToParentStatus.Merged)
          sb.append("m");
        sb.append("-");
      }

      sb.append(branch.getName());

      var currBranch = gitMacheteRepository.getCurrentBranchIfManaged();
      if (currBranch.isPresent() && currBranch.get().equals(branch))
        sb.append(" *");

      if (branch.getCustomAnnotation().isPresent()) {
        sb.append("  ");
        sb.append(branch.getCustomAnnotation().get());
      }
      var originSync = branch.computeSyncToOriginStatus();
      if (originSync != SyncToOriginStatus.InSync) {
        sb.append(" (");
        if (originSync == SyncToOriginStatus.Ahead)
          sb.append("ahead of origin");
        if (originSync == SyncToOriginStatus.Behind)
          sb.append("behind origin");
        if (originSync == SyncToOriginStatus.Untracked)
          sb.append("untracked");
        if (originSync == SyncToOriginStatus.Diverged)
          sb.append("diverged from origin");
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
