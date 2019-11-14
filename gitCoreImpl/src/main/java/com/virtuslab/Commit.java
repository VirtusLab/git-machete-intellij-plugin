package com.virtuslab;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Date;

@EqualsAndHashCode
public class Commit implements ICommit{
    @Getter
    private RevCommit jgitCommit;

    public Commit(RevCommit commit) {
        if(commit == null)
            throw new NullPointerException("JGit commit passed to Commit constructor cannot be null");
        this.jgitCommit = commit;
    }

    @Override
    public String getMessage() {
        return jgitCommit.getFullMessage();
    }

    @Override
    public PersonIdentity getAuthor() {
        return new PersonIdentity(jgitCommit.getAuthorIdent());
    }

    @Override
    public PersonIdentity getCommitter() {
        return new PersonIdentity(jgitCommit.getCommitterIdent());
    }

    @Override
    public Date getCommitTime() {
        return new Date(jgitCommit.getCommitTime());
    }

    @Override
    public CommitHash getHash() {
        return new CommitHash(jgitCommit.getId().getName());
    }


    @Override
    public String toString() {
        return jgitCommit.getId().getName().substring(0, 7)+": "+jgitCommit.getShortMessage();
    }
}
