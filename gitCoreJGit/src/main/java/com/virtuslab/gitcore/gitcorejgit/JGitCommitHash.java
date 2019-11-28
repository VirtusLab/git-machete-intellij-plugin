package com.virtuslab.gitcore.gitcorejgit;


import com.virtuslab.gitcore.gitcoreapi.IGitCoreCommitHash;


public class JGitCommitHash extends JGitObjectHash implements IGitCoreCommitHash {
    public JGitCommitHash(String hashString) {
        super(hashString);
    }
}
