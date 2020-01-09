package com.virtuslab.gitcore.gitcoreapi;

import java.nio.file.Path;

public interface IGitCoreSubmoduleEntry {
  Path getPath();

  String getName();
}
