package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IBranchTrackingStatus;
import com.virtuslab.gitcore.gitcoreapi.ILocalBranch;
import org.eclipse.jgit.lib.BranchTrackingStatus;

import java.io.IOException;
import java.util.Optional;

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

    @Override
    public Optional<IBranchTrackingStatus> getTrackingStatus() throws JGitException {
        BranchTrackingStatus ts;
        try {
            ts = BranchTrackingStatus.of(repo.getJgitRepo(), getName());
        } catch(IOException e) {
            throw new JGitException(e);
        }

        if(ts == null)
            return Optional.empty();

        return Optional.of(JGitBranchTrackingStatus.build(ts.getAheadCount(), ts.getBehindCount()));
    }
}
