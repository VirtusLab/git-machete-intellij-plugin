package com.virtuslab.gitmachete.gitcorejgit;


import com.virtuslab.gitmachete.gitcore.ICommitHash;


public class JGitCommitHash extends JGitObjectHash implements ICommitHash {
    public JGitCommitHash(String hashString) {
        super(hashString);
    }
}
