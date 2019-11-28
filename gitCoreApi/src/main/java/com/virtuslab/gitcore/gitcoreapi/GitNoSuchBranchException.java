package com.virtuslab.gitcore.gitcoreapi;

public class GitNoSuchBranchException extends GitException {
    public GitNoSuchBranchException() {
        super();
    }

    public GitNoSuchBranchException(String message) {
        super(message);
    }

    public GitNoSuchBranchException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitNoSuchBranchException(Throwable cause) {
        super(cause);
    }
}
