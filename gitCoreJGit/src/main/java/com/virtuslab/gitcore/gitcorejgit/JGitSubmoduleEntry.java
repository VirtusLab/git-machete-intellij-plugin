package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.ISubmoduleEntry;
import lombok.Getter;

import java.nio.file.Path;

@Getter
public class JGitSubmoduleEntry implements ISubmoduleEntry {
    private String name;
    private Path path;

    public JGitSubmoduleEntry(String name, Path path) {
        this.name = name;
        this.path = path;
    }
}
