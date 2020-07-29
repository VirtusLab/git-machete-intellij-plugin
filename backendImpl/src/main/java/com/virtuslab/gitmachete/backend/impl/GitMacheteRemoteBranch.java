package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;

@Data
public class GitMacheteRemoteBranch implements IGitMacheteRemoteBranch {
  private final String name;
  private final IGitMacheteCommit pointedCommit;
}
