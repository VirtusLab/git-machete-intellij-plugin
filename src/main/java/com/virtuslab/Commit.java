package com.virtuslab;

import org.eclipse.jgit.lib.ObjectId;

public class Commit {
    private ObjectId id;
    private String commitMessage;

    public Commit(ObjectId id, String commitMessage) {
        this.id = id;
        this.commitMessage = commitMessage;
    }

    public ObjectId getId() {
        return id;
    }
    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getCommitMessage() {
        return commitMessage;
    }
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    @Override
    public String toString() {
        return id.getName().substring(0, 7)+": "+commitMessage;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;

        if(!(o instanceof Commit))
            return false;

        return id.equals(((Commit) o).getId()) && commitMessage.equals(((Commit) o).commitMessage);
    }

    @Override
    public int hashCode() {
        return 17 * id.hashCode() + 31 * commitMessage.hashCode();
    }
}
