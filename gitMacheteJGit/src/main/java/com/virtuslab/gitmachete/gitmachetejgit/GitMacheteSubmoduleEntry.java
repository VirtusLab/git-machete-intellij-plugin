package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import java.nio.file.Path;
import lombok.Data;

@Data
public class GitMacheteSubmoduleEntry implements IGitMacheteSubmoduleEntry {
  private final Path path;
  private final String name;
}
