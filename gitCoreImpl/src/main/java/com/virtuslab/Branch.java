package com.virtuslab;

import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import java.io.IOException;
import java.text.MessageFormat;

public class Branch implements IBranch {
    private String branchName;
    private Repository repo;

    public Branch(Repository repo, String branchName) {
        this.repo = repo;
        this.branchName = branchName;
    }

    @Override
    public String getName() {
        return branchName;
    }

    @Override
    public ICommit getPointedCommit() throws JGitException {
        org.eclipse.jgit.lib.Repository jgitRepo = repo.getJgitRepo();
        RevWalk rw = new RevWalk(jgitRepo);
        RevCommit c;
        try {
            c = rw.parseCommit(jgitRepo.resolve("refs/heads/" + branchName));
        }
        catch(Exception e) {
            throw new JGitException(e);
        }

        return new Commit(c);
    }

    @Override
    public ICommit getForkPoint(IBranch branch) throws GitException {
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

        var forkPointIterator = walk.iterator();

        if(!forkPointIterator.hasNext())
            throw new GitNoForkPointException(MessageFormat.format("Branches \"{0}\" and \"{1}\" don't have any fork points", this.getName(), branch.getName()));

        return new Commit(forkPointIterator.next());
    }
}
