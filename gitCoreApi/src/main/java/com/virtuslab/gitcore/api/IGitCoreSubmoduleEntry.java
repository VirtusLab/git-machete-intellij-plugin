package com.virtuslab.gitcore.api;

import java.nio.file.Path;

public interface IGitCoreSubmoduleEntry {
  Path getPath();

  String getName();
}
