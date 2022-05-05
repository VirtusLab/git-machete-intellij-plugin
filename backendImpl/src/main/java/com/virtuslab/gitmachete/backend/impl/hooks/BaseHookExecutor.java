package com.virtuslab.gitmachete.backend.impl.hooks;

import java.io.File;

abstract class BaseHookExecutor {
  protected static final String NL = System.lineSeparator();

  protected final File rootDirectory;
  protected final File mainGitDirectory;
  protected final File hookFile;

  protected BaseHookExecutor(File rootDirectory, File mainGitDirectory, File hookFile) {
    this.rootDirectory = rootDirectory;
    this.mainGitDirectory = mainGitDirectory;
    this.hookFile = hookFile;
  }
}
