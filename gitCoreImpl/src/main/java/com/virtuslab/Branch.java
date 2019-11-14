package com.virtuslab;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.ReflogEntry;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;


@EqualsAndHashCode
@AllArgsConstructor
public abstract class Branch implements IBranch {
    @Getter
    private static String branchesPath = "asdsdfsdfsdf ";

    protected Repository repo;
    protected String branchName;

    @Override
    public String getName() {
        return branchName;
    }

    public String getPath() {
        if(this instanceof LocalBranch)
            return LocalBranch.getBranchesPath()+branchName;
        if(this instanceof RemoteBranch)
            return RemoteBranch.getBranchesPath()+branchName;

        return getBranchesPath()+branchName;
    }

    @Override
    public Commit getPointedCommit() throws GitException {
        return new Commit(getPointedRevCommit());
    }


    protected RevCommit getPointedRevCommit() throws GitException {
        org.eclipse.jgit.lib.Repository jgitRepo = repo.getJgitRepo();
        RevWalk rw = new RevWalk(jgitRepo);
        RevCommit c;
        String brPath = (this instanceof LocalBranch ? LocalBranch.getBranchesPath() : RemoteBranch.getBranchesPath());
        try {
            ObjectId o =jgitRepo.resolve(brPath + branchName);
            if(o == null)
                throw new GitNoSuchBranchException(MessageFormat.format("{1} branch \"{0}\" does not exist in this repository", branchName, this instanceof LocalBranch ? "Local" : "Remote"));
            c = rw.parseCommit(o);
        }
        catch (MissingObjectException | IncorrectObjectTypeException e) {
            throw new GitNoSuchCommitException(MessageFormat.format("Commit pointed by {1} branch \"{0}\" does not exist in this repository", branchName, this instanceof LocalBranch ? "Local" : "Remote"));
        }
        catch(RevisionSyntaxException | IOException e) {
            throw new JGitException(e);
        }

        return c;
    }


    @Override
    public Optional<ICommit> getMergeBase(IBranch branch) throws GitException {
        RevWalk walk = new RevWalk(repo.getJgitRepo());
        walk.setRevFilter(RevFilter.MERGE_BASE);
        try {
            walk.markStart(((Commit) this.getPointedCommit()).getJgitCommit());
            walk.markStart(((Commit) branch.getPointedCommit()).getJgitCommit());
        }
        catch (Exception e)
        {
            throw new JGitException(e);
        }

        var mergeBaseIterator = walk.iterator();

        if(!mergeBaseIterator.hasNext())
            return Optional.empty();

        return Optional.of(new Commit(mergeBaseIterator.next()));
    }


    @Override
    public Optional<ICommit> getForkPoint(IBranch parentBranch) throws GitException{
        Collection<ReflogEntry> reflog = null;
        try {
            reflog = repo.getJgitGit().reflog().setRef(parentBranch.getPath()).call();
        }
        catch (Exception e) {
            throw new JGitException(e);
        }

        RevWalk walk = new RevWalk(repo.getJgitRepo());
        RevCommit commit = getPointedRevCommit();
        try {
            walk.markStart(commit);
        }
        catch (Exception e) {
            throw new JGitException(e);
        }

        for(var curBranchCommit : walk) {
            for(var parentBranchReflogEntry : reflog) {
                if(curBranchCommit.getId().equals(parentBranchReflogEntry.getNewId())) {
                    return Optional.of(new Commit(curBranchCommit));
                }
            }
        }

        return Optional.empty();
    }
}
