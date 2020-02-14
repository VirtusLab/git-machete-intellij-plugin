package com.virtuslab.gitmachete.gitmacheteapi;

public interface IGitMergeParameters {
  IGitMacheteBranch getCurrentBranch();

  IGitMacheteBranch getUpstreamBranch();
}
