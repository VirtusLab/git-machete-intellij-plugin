package com.virtuslab.gitmachete.gitmachetejgit;

import java.nio.file.Path;

import lombok.Data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;

@Data
public class GitMacheteSubmoduleEntry implements IGitMacheteSubmoduleEntry {
	private final Path path;
	private final String name;
}
