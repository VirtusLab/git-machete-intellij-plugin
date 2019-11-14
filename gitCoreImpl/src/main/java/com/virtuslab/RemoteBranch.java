package com.virtuslab;

import lombok.AccessLevel;
import lombok.Getter;

public class RemoteBranch extends Branch implements IRemoteBranch {
    @Getter
    private static final String branchesPath = "refs/remotes/";

    public RemoteBranch(Repository repo, String branchName) {
        super(repo, branchName);
    }
}
