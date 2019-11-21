package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.Branch;
import com.virtuslab.gitmachete.gitmacheteapi.Commit;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
public class GitMacheteBranch implements Branch {
    private String name;
    List<Commit> commits = new LinkedList<>();
    @Getter(AccessLevel.NONE) List<Branch> childrenBranches = new LinkedList<>();
    SyncToParentStatus syncToParentStatus = null;
    SyncToOriginStatus syncToOriginStatus = null;

    public GitMacheteBranch(String name) {
        this.name = name;
    }

    public List<Branch> getBranches() {
        return childrenBranches;
    }
}
