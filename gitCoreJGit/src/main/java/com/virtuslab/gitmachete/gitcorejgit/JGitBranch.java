package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;


@EqualsAndHashCode
@AllArgsConstructor
public abstract class JGitBranch implements IBranch {
    protected JGitRepository repo;
    protected String branchName;

    @Override
    public String getName() {
        return branchName;
    }

    public String getFullName() {
        return getBranchesPath()+branchName;
    }

    public abstract String getBranchesPath();

    @Override
    public abstract boolean isLocal();

    public abstract String getBranchTypeString();

    public abstract String getBranchTypeString(boolean capitalized);

    @Override
    public JGitCommit getPointedCommit() throws GitException {
        return new JGitCommit(getPointedRevCommit());
    }


    protected RevCommit getPointedRevCommit() throws GitException {
        org.eclipse.jgit.lib.Repository jgitRepo = repo.getJgitRepo();
        RevWalk rw = new RevWalk(jgitRepo);
        RevCommit c;
        try {
            ObjectId o =jgitRepo.resolve(getFullName());
            if(o == null)
                throw new GitNoSuchBranchException(MessageFormat.format("{1} branch \"{0}\" does not exist in this repository", branchName, getBranchTypeString(true)));
            c = rw.parseCommit(o);
        }
        catch (MissingObjectException | IncorrectObjectTypeException e) {
            throw new GitNoSuchCommitException(MessageFormat.format("Commit pointed by {1} branch \"{0}\" does not exist in this repository", branchName, getBranchTypeString()));
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
            walk.markStart(this.getPointedCommit().getJgitCommit());

            String commitHash = branch.getPointedCommit().getHash().getHashString();
            ObjectId objectId = repo.getJgitRepo().resolve(commitHash);
            walk.markStart(walk.parseCommit(objectId));
        }
        catch (Exception e)
        {
            throw new JGitException(e);
        }

        var mergeBaseIterator = walk.iterator();

        if(!mergeBaseIterator.hasNext())
            return Optional.empty();

        return Optional.of(new JGitCommit(mergeBaseIterator.next()));
    }


    @Override
    public Optional<ICommit> getForkPoint(IBranch parentBranch) throws GitException{
        Collection<ReflogEntry> reflog;
        try {
            reflog = repo.getJgitGit().reflog().setRef(parentBranch.getFullName()).call();
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

        ReflogEntry[] refEntrys = reflog.toArray(new ReflogEntry[0]);

        for(var curBranchCommit : walk) {
            for(var parentBranchReflogEntry : refEntrys) {
                if(curBranchCommit.getId().equals(parentBranchReflogEntry.getNewId())) {
                    return Optional.of(new JGitCommit(curBranchCommit));
                }
            }
        }

        return Optional.empty();
    }
}
