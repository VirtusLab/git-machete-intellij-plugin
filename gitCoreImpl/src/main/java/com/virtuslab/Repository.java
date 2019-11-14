package com.virtuslab;

import lombok.Getter;
import org.eclipse.jgit.api.Git;
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
    @Getter
    private org.eclipse.jgit.lib.Repository jgitRepo;
    @Getter
    private Git jgitGit;

    public Repository(String pathToRootOfRepository) throws IOException {
        jgitRepo = new FileRepository(Paths.get(pathToRootOfRepository, ".git").toString());
        jgitGit = new Git(jgitRepo);
    }

    @Override
    public Optional<ILocalBranch> getCurrentBranch() throws JGitException {
        Ref r;
        try {
            r = jgitRepo.getRef(Constants.HEAD);
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
        if(!checkIfBranchExist(LocalBranch.getBranchesPath() + branchName))
            throw new GitNoSuchBranchException(MessageFormat.format("Local branch \"{0}\" does not exist in this repository", branchName));

        return new LocalBranch(this, branchName);
    }

    @Override
    public RemoteBranch getRemoteBranch(String branchName) throws GitException{
        if(!checkIfBranchExist(RemoteBranch.getBranchesPath() + branchName))
            throw new GitNoSuchBranchException(MessageFormat.format("Remote branch \"{0}\" does not exist in this repository", branchName));

        return new RemoteBranch(this, branchName);
    }



    private boolean checkIfBranchExist(String path) throws JGitException{
        RevWalk rw = new RevWalk(jgitRepo);
        RevCommit c;
        try {
            ObjectId o = jgitRepo.resolve(path);

            return o != null;
        }
        catch (RevisionSyntaxException | IOException e) {
            throw  new JGitException(e);
        }
    }
}
