package com.virtuslab.gitcore.gitcoreapi;

import java.util.Date;

public interface ICommit {
    String getMessage() throws GitException;
    IPersonIdentity getAuthor() throws GitException;
    IPersonIdentity getCommitter() throws GitException;
    Date getCommitTime() throws GitException;
    ICommitHash getHash() throws GitException;
    boolean isAncestorOf(ICommit parentCommit) throws GitException;
}
