package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.IBranchTrackingStatus;
import com.virtuslab.gitmachete.gitcore.ILocalBranch;
import org.eclipse.jgit.lib.BranchTrackingStatus;

import java.io.IOException;

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
    public IBranchTrackingStatus getTrackingStatus() throws JGitException {
        BranchTrackingStatus ts;
        try {
            ts = BranchTrackingStatus.of(repo.getJgitRepo(), getName());
        } catch(IOException e) {
            throw new JGitException(e);
        }

        if(ts == null)
            return JGitBranchTrackingStatus.buildUntracked();

        return JGitBranchTrackingStatus.buildTracked(ts.getAheadCount(), ts.getBehindCount());
    }
}
