package com.virtuslab;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Optional;

public class Repository implements IRepository {
    private org.eclipse.jgit.lib.Repository repo;

    public Repository(String pathToRootOfRepository) throws IOException {
        repo = new FileRepository(Paths.get(pathToRootOfRepository, ".git").toString());
    }

    @Override
    public Optional<ILocalBranch> getCurrentBranch() throws JGitException {
        Ref r;
        try {
            r = repo.getRef(Constants.HEAD);
        }
        catch (IOException e) {
            throw new JGitException("Cannot get current branch", e);
        }

        if(r == null)
            throw new JGitException("Error occur while getting current branch ref");

        if(r.isSymbolic())
            return Optional.of(new LocalBranch(this, org.eclipse.jgit.lib.Repository.shortenRefName(r.getTarget().getName())));

        return Optional.empty();
    }

    @Override
    public LocalBranch getLocalBranch(String branchName) throws GitException{
        if(!checkIfBranchExist(ILocalBranch.branchesPath + branchName))
            throw new GitNoSuchBranchException(MessageFormat.format("Local branch \"{0}\" does not exist in this repository", branchName));

        return new LocalBranch(this, branchName);
    }

    @Override
    public RemoteBranch getRemoteBranch(String branchName) throws GitException{
        if(!checkIfBranchExist(ILocalBranch.branchesPath + branchName))
            throw new GitNoSuchBranchException(MessageFormat.format("Remote branch \"{0}\" does not exist in this repository", branchName));

        return new RemoteBranch(this, branchName);
    }


    public org.eclipse.jgit.lib.Repository getJgitRepo() {
        return repo;
    }



    private boolean checkIfBranchExist(String path) throws JGitException{
        RevWalk rw = new RevWalk(repo);
        RevCommit c;
        try {
            ObjectId o = repo.resolve(path);

            return o != null;
        }
        catch (RevisionSyntaxException | IOException e) {
            throw  new JGitException(e);
        }
    }
}
