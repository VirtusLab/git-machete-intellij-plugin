package com.virtuslab.gitcore.gitcoreapi;

import java.nio.file.Path;

public interface IGitCoreSubmoduleEntry {
  String getName();

  Path getPath();
}
