package com.virtuslab.gitcore.gitcoreapi;

public abstract class GitException extends Exception {
    public GitException() {
        super();
    }

    public GitException(String message) {
        super(message);
    }

    public GitException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitException(Throwable cause) {
        super(cause);
    }
}
