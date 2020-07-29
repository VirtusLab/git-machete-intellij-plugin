package com.virtuslab.gitmachete.backend.api;

public interface IGitMacheteRemoteBranch {
  String getName();

  IGitMacheteCommit getPointedCommit();
}
