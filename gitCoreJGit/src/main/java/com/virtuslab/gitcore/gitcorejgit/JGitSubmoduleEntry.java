package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreSubmoduleEntry;
import java.nio.file.Path;
import lombok.Data;

@Data
public class JGitSubmoduleEntry implements IGitCoreSubmoduleEntry {
  private final Path path;
  private final String name;
}
