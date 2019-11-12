package com.virtuslab;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Date;

public class Commit implements ICommit{
    private RevCommit jgitCommit;
    private CommitHash commitHash;

    public Commit(RevCommit commit) {
        if(commit == null)
            throw new NullPointerException("JGit commit passed to Commit constructor cannot be null");
        this.jgitCommit = commit;
        commitHash = new CommitHash(jgitCommit.getId().getName());
    }

    @Override
    public String getMessage() {
        return jgitCommit.getFullMessage();
    }

    @Override
    public IPersonIdentity getAuthor() {
        return new PersonIdentity(jgitCommit.getAuthorIdent());
    }

    @Override
    public IPersonIdentity getCommitter() {
        return new PersonIdentity(jgitCommit.getCommitterIdent());
    }

    @Override
    public Date getCommitTime() {
        return new Date(jgitCommit.getCommitTime());
    }

    @Override
    public ICommitHash getHash() {
        return commitHash;
    }


    @Override
    public String toString() {
        return jgitCommit.getId().getName().substring(0, 7)+": "+jgitCommit.getShortMessage();
    }

    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;

        if(!(o instanceof Commit))
            return false;

        return commitHash.equals(o);
    }

    @Override
    public int hashCode() {
        return commitHash.hashCode();
    }

    public RevCommit getJgitCommit() {
        return jgitCommit;
    }
}
