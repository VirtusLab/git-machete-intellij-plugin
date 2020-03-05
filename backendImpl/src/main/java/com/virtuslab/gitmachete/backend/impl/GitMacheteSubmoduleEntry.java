package com.virtuslab.gitmachete.backend.impl;

import java.nio.file.Path;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.IGitMacheteSubmoduleEntry;

@Data
public class GitMacheteSubmoduleEntry implements IGitMacheteSubmoduleEntry {
  private final Path path;
  private final String name;
}
