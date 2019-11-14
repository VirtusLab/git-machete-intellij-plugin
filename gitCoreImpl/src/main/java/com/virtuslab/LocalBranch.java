package com.virtuslab;

public class LocalBranch extends Branch implements ILocalBranch {
    public LocalBranch(Repository repo, String branchName) {
        super(repo, branchName);
    }
}
