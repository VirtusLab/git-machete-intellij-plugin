package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GitMacheteSubmoduleEntry implements IGitMacheteSubmoduleEntry {
  private final Path path;
  private final String name;
}
