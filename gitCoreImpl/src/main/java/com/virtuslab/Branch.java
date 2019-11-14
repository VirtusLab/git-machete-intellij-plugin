package com.virtuslab;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import java.io.IOException;
import java.text.MessageFormat;


@EqualsAndHashCode
@AllArgsConstructor
public abstract class Branch implements IBranch {
    private Repository repo;
    private String branchName;

    @Override
    public String getName() {
        return branchName;
    }

    @Override
    public Commit getPointedCommit() throws GitException {
        org.eclipse.jgit.lib.Repository jgitRepo = repo.getJgitRepo();
        RevWalk rw = new RevWalk(jgitRepo);
        RevCommit c;
        try {
            ObjectId o =jgitRepo.resolve(branchesPath + branchName);
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

        return new Commit(c);
    }

    @Override
    public Commit getMergeBase(IBranch branch) throws GitException {
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
            throw new GitNoForkPointException(MessageFormat.format("Branches \"{0}\" and \"{1}\" don't have merge base", this.getName(), branch.getName()));

        return new Commit(mergeBaseIterator.next());
    }
}
