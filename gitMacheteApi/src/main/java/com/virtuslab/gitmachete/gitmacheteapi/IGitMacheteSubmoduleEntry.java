package com.virtuslab.gitmachete.gitmacheteapi;

import java.nio.file.Path;

public interface IGitMacheteSubmoduleEntry {
  Path getPath();

  String getName();
}
