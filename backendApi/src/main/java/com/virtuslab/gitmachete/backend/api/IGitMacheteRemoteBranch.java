package com.virtuslab.gitmachete.backend.api;

public interface IGitMacheteRemoteBranch {
  String getName();

  String getFullName();

  String getFullNameAsLocalBranchOnRemote();

  String getRemoteName();
}
