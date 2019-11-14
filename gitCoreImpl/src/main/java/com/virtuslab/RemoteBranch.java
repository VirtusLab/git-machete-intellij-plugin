package com.virtuslab;

public class RemoteBranch extends Branch implements IRemoteBranch {
    public RemoteBranch(Repository repo, String branchName) {
        super(repo, branchName);
    }
}
