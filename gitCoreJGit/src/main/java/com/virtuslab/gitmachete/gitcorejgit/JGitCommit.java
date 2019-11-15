package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.ICommit;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Date;

@EqualsAndHashCode
public class JGitCommit implements ICommit {
    @Getter
    private RevCommit jgitCommit;

    public JGitCommit(RevCommit commit) {
        if(commit == null)
            throw new NullPointerException("JGit commit passed to Commit constructor cannot be null");
        this.jgitCommit = commit;
    }

    @Override
    public String getMessage() {
        return jgitCommit.getFullMessage();
    }

    @Override
    public JGitPersonIdentity getAuthor() {
        return new JGitPersonIdentity(jgitCommit.getAuthorIdent());
    }

    @Override
    public JGitPersonIdentity getCommitter() {
        return new JGitPersonIdentity(jgitCommit.getCommitterIdent());
    }

    @Override
    public Date getCommitTime() {
        return new Date(jgitCommit.getCommitTime());
    }

    @Override
    public JGitCommitHash getHash() {
        return new JGitCommitHash(jgitCommit.getId().getName());
    }


    @Override
    public String toString() {
        return jgitCommit.getId().getName().substring(0, 7)+": "+jgitCommit.getShortMessage();
    }
}
