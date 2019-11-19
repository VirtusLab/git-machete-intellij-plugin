package com.virtuslab.gitmachete.gitmachetejgit;

public class LevelsMismatchException extends GitMacheteJGitException {
    public LevelsMismatchException() {
        super();
    }

    public LevelsMismatchException(String message) {
        super(message);
    }

    public LevelsMismatchException(Throwable e) {
        super(e);
    }

    public LevelsMismatchException(String message, Throwable e) {
        super(message, e);
    }
}
