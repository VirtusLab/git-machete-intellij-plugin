package com.virtuslab;

public class CommitHash implements ICommitHash {
    private String hash;

    public CommitHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;

        if(!(o instanceof CommitHash))
            return false;

        return hash.equals(((CommitHash) o).hash);
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }
}
