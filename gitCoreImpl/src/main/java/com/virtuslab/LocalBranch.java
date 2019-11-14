package com.virtuslab;

import lombok.AccessLevel;
import lombok.Getter;

public class LocalBranch extends Branch implements ILocalBranch {
    @Getter
    private static final String branchesPath = "refs/heads/";

    public LocalBranch(Repository repo, String branchName) {
        super(repo, branchName);
    }

}
