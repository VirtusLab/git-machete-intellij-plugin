package com.virtuslab;

public class GitNoForkPointException extends GitImplException {
    public GitNoForkPointException() {
        super();
    }

    public GitNoForkPointException(String message) {
        super(message);
    }

    public GitNoForkPointException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitNoForkPointException(Throwable cause) {
        super(cause);
    }
}
