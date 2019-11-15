package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.IRemoteBranch;

public class JGitRemoteBranch extends JGitBranch implements IRemoteBranch {
    public static String branchesPath = "refs/remotes/";

    public JGitRemoteBranch(JGitRepository repo, String branchName) {
        super(repo, branchName);
    }

    @Override
    public String getBranchesPath() {
        return branchesPath;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public String getBranchTypeString() {
        return getBranchTypeString(false);
    }

    @Override
    public String getBranchTypeString(boolean capitalized) {
        if(capitalized)
            return "Remote";
        else
            return "remote";
    }
}
