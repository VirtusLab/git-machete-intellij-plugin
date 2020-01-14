package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreSubmoduleEntry;
import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class JGitSubmoduleEntry implements IGitCoreSubmoduleEntry {
  private Path path;
  private String name;

  public JGitSubmoduleEntry(Path path, String name) {
    this.path = path;
    this.name = name;
  }
}
