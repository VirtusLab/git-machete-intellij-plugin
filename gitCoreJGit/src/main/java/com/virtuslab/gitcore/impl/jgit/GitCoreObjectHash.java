package com.virtuslab.gitcore.impl.jgit;

import lombok.Data;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;

@Data
public class GitCoreObjectHash implements IGitCoreCommitHash {
	private final String hashString;
}
