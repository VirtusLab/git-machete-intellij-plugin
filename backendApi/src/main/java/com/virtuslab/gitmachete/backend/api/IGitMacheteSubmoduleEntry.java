package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

public interface IGitMacheteSubmoduleEntry {
  Path getPath();

  String getName();
}
