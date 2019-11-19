package com.virtuslab.gitmachete.gitmachetejgit;

public class IdentsMismatchException extends GitMacheteJGitException {
    public IdentsMismatchException() {
        super();
    }

    public IdentsMismatchException(String message) {
        super(message);
    }

    public IdentsMismatchException(Throwable e) {
        super(e);
    }

    public IdentsMismatchException(String message, Throwable e) {
        super(message, e);
    }
}
