package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;

public class GitMacheteJGitException extends GitMacheteException {
    public GitMacheteJGitException() {
        super();
    }

    public GitMacheteJGitException(String message) {
        super(message);
    }

    public GitMacheteJGitException(Throwable e) {
        super(e);
    }

    public GitMacheteJGitException(String message, Throwable e) {
        super(message, e);
    }
}
