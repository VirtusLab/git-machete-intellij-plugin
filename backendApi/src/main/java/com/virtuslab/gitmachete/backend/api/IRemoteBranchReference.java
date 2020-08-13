package com.virtuslab.gitmachete.backend.api;

public interface IRemoteBranchReference {
  String getName();

  String getFullName();

  String getFullNameAsLocalBranchOnRemote();

  String getRemoteName();
}
