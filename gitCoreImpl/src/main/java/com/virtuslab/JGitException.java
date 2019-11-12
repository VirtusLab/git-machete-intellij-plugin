package com.virtuslab;

public class JGitException extends GitException {
    public JGitException() {
        super();
    }

    public JGitException(String message) {
        super(message);
    }

    public JGitException(String message, Throwable cause) {
        super(message, cause);
    }

    public JGitException(Throwable cause) {
        super(cause);
    }
}
