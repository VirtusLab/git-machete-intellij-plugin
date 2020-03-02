package com.virtuslab.gitcore.impl.jgit;

import java.nio.file.Path;

import lombok.Data;

import com.virtuslab.gitcore.api.IGitCoreSubmoduleEntry;

@Data
public class GitCoreSubmoduleEntry implements IGitCoreSubmoduleEntry {
	private final Path path;
	private final String name;
}
