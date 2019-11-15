package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.ILocalBranch;

public class JGitLocalBranch extends JGitBranch implements ILocalBranch {
    public static String branchesPath = "refs/heads/";

    public JGitLocalBranch(JGitRepository repo, String branchName) {
        super(repo, branchName);
    }

    @Override
    public String getBranchesPath() {
        return branchesPath;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public String getBranchTypeString() {
        return getBranchTypeString(false);
    }

    @Override
    public String getBranchTypeString(boolean capitalized) {
        if(capitalized)
            return "Local";
        else
            return "local";
    }
}
