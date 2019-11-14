package com.virtuslab;

public class GitNoSuchBranchException extends GitImplException {
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
