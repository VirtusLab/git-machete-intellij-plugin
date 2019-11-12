package com.virtuslab;

import org.eclipse.jgit.storage.file.FileRepository;

import java.io.IOException;
import java.nio.file.Paths;

public class Repository implements IRepository {
    private org.eclipse.jgit.lib.Repository repo;

    public Repository(String pathToRootOfRepository) throws IOException {
        repo = new FileRepository(Paths.get(pathToRootOfRepository, ".git").toString());
    }

    @Override
    public IBranch getCurrentBranch() throws JGitException {
        try {
            return getBranch(repo.getBranch());
        }
        catch (IOException e) {
            throw new JGitException("Cannot get current branch", e);
        }
    }

    @Override
    public IBranch getBranch(String branchName) {
        return new Branch(this, branchName);
    }

    public org.eclipse.jgit.lib.Repository getJgitRepo() {
        return repo;
    }
}
