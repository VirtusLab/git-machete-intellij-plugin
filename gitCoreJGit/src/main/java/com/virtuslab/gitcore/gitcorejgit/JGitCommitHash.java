package com.virtuslab.gitcore.gitcorejgit;


import com.virtuslab.gitcore.gitcoreapi.ICommitHash;


public class JGitCommitHash extends JGitObjectHash implements ICommitHash {
    public JGitCommitHash(String hashString) {
        super(hashString);
    }
}
