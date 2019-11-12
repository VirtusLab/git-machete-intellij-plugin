package com.virtuslab;

public class GitImplException extends GitException {
    public GitImplException() {
        super();
    }

    public GitImplException(String message) {
        super(message);
    }

    public GitImplException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitImplException(Throwable cause) {
        super(cause);
    }
}
