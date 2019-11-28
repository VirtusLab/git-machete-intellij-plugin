package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.List;

public interface IGitMacheteBranch {
    String getName();

    List<IGitMacheteCommit> getCommits();

    List<IGitMacheteBranch> getBranches();

    SyncToParentStatus getSyncToParentStatus();

    SyncToOriginStatus getSyncToOriginStatus();
}
