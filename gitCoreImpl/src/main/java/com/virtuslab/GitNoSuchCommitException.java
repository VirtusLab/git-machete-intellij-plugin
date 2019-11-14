package com.virtuslab;

public class GitNoSuchCommitException extends GitImplException {
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
