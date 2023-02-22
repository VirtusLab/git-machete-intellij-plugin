package com.virtuslab.gitcore.impl.jgit;

import java.nio.file.Path;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitcore.api.IGitCoreRepositoryFactory;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class GitCoreRepositoryFactory implements IGitCoreRepositoryFactory {
  @UIThreadUnsafe
  public IGitCoreRepository create(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath)
      throws GitCoreException {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      if (!stackTrace.contains("at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed")) {
        System.out.println("Expected non-EDT:");
        System.out.println(stackTrace);
        throw new RuntimeException("Expected EDT: " + stackTrace);
      }
    }
    return new GitCoreRepository(rootDirectoryPath, mainGitDirectoryPath, worktreeGitDirectoryPath);
  }
}
