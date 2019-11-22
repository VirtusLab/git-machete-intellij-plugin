package com.virtuslab.gitcore.gitcoreapi;

public class GitNoSuchCommitException extends GitException {
    public GitNoSuchCommitException() {
        super();
    }

    public GitNoSuchCommitException(String message) {
        super(message);
    }

    public GitNoSuchCommitException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitNoSuchCommitException(Throwable cause) {
        super(cause);
    }
}
