package com.virtuslab.gitcore.impl.jgit;

import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;
import java.nio.file.Path;
import lombok.Data;

@Data
public class GitCoreSubmoduleEntry implements IGitCoreSubmoduleEntry {
  private final Path path;
  private final String name;
}
