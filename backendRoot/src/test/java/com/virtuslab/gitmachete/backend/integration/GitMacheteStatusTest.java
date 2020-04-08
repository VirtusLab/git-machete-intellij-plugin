package com.virtuslab.gitmachete.backend.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.root.GitMacheteRepositoryBuilder;

public class GitMacheteStatusTest {
  IGitMacheteRepository gitMacheteRepository = null;

  public static final Path tmpTestDir = Paths.get("/tmp/machete-tests");
  public static final Path scriptsDir = tmpTestDir.resolve("scripts");
  public static final Path repositoryBuildingScript = scriptsDir.resolve("repo.sh");
  public static final Path repositoryDir = tmpTestDir.resolve("machete-sandbox");
  public static final String repositoryPreparingCommand = String.format("/bin/bash %s %s",
      repositoryBuildingScript.toAbsolutePath().toString(), tmpTestDir.toAbsolutePath().toString());

  GitMacheteRepositoryBuilder gitMacheteRepositoryBuilder = new GitMacheteRepositoryBuilder(repositoryDir);

  @Before
  public void init() throws Exception {
    // Prepare repo
    createDirStructure();
    copyScriptFromResources("repo1.sh");
    prepareRepoFromScript();

    gitMacheteRepository = gitMacheteRepositoryBuilder.build();
  }

  @After
  public void cleanup() throws IOException {
    Files.walk(tmpTestDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

  @Test
  public void statusTest() throws Exception {
    String myResult = repositoryStatus();
    String gitMacheteCliStatus = gitMacheteCliStatus();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println("OUR OUTPUT:");
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
    var branches = gitMacheteRepository.getRootBranches();
    int lastRootBranchIndex = branches.size() - 1;
    for (int currentRootBranch = 0; currentRootBranch <= lastRootBranchIndex; currentRootBranch++) {
      var b = branches.get(currentRootBranch);
      printRootBranch(b, sb);
      if (currentRootBranch < lastRootBranchIndex)
        sb.append(System.lineSeparator());
    }

    return sb.toString();
  }

  private void printRootBranch(BaseGitMacheteRootBranch branch, StringBuilder sb) {
    sb.append("  ");
    printCommonParts(branch, /* level */ 0, sb);
  }

  private void printNonRootBranch(BaseGitMacheteNonRootBranch branch, int level, StringBuilder sb) {
    sb.append("  ");

    sb.append("| ".repeat(level));

    sb.append(System.lineSeparator());

    var commits = branch.getCommits().reverse();

    for (var c : commits) {
      sb.append("  ");
      sb.append("| ".repeat(level));
      sb.append(c.getMessage().split("\n", 2)[0]);
      sb.append(System.lineSeparator());
    }

    sb.append("  ");
    sb.append("| ".repeat(level - 1));

    var parentStatus = branch.getSyncToParentStatus();
    if (parentStatus == SyncToParentStatus.InSync)
      sb.append("o");
    else if (parentStatus == SyncToParentStatus.OutOfSync)
      sb.append("x");
    else if (parentStatus == SyncToParentStatus.InSyncButForkPointOff)
      sb.append("?");
    else if (parentStatus == SyncToParentStatus.Merged)
      sb.append("m");
    sb.append("-");

    printCommonParts(branch, level, sb);
  }

  private void printCommonParts(BaseGitMacheteBranch branch, int level, StringBuilder sb) {
    sb.append(branch.getName());

    var currBranch = gitMacheteRepository.getCurrentBranchIfManaged();
    if (currBranch.isPresent() && currBranch.get().equals(branch))
      sb.append(" *");

    var customAnnotation = branch.getCustomAnnotation();
    if (customAnnotation.isPresent()) {
      sb.append("  ");
      sb.append(customAnnotation.get());
    }
    var originSync = branch.getSyncToOriginStatus();
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

    for (var b : branch.getDownstreamBranches()) {
      printNonRootBranch(b, /* level */ level + 1, sb);
    }
  }

  private static String convertStreamToString(InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
