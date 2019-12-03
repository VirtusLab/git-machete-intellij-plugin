package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreSubmoduleEntry;
import java.nio.file.Path;
import lombok.Getter;

@Getter
public class JGitSubmoduleEntry implements IGitCoreSubmoduleEntry {
  private String name;
  private Path path;

  public JGitSubmoduleEntry(String name, Path path) {
    this.name = name;
    this.path = path;
  }
}
