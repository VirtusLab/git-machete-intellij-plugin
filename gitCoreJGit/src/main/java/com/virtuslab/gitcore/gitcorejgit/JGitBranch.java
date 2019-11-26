package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;


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
        return new JGitCommit(getPointedRevCommit(), repo);
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
        } catch (MissingObjectException | IncorrectObjectTypeException e) {
            throw new GitNoSuchCommitException(MessageFormat.format("Commit pointed by {1} branch \"{0}\" does not exist in this repository", branchName, getBranchTypeString()));
        } catch(RevisionSyntaxException | IOException e) {
            throw new JGitException(e);
        }

        return c;
    }

    public Optional<ICommit> getMergeBase(IBranch branch) throws GitException {
        RevWalk walk = new RevWalk(repo.getJgitRepo());

        walk.sort(RevSort.TOPO, true);
        walk.sort(RevSort.COMMIT_TIME_DESC, true);

        try {
            /*
              I mark both commits as a start commit, because I want to traverse tree of commits starting from that points.
              In every iteration iterator from "walk" give me next commit from one of this path depend on the commit date.
              In every iteration of iterator I try add current commit's parent(s) (actually their ObjectId's) to "ancestorsOfStartCommits" list.
              If one of parent's ObjectId is already in this list, that mean (in consideration of this sorting) that it's a merge base (the first common ancestor)
             */
            walk.markStart(this.getPointedCommit().getJgitCommit());

            String commitHash = branch.getPointedCommit().getHash().getHashString();
            ObjectId objectId = repo.getJgitRepo().resolve(commitHash);
            walk.markStart(walk.parseCommit(objectId));
        } catch (Exception e) {
            throw new JGitException(e);
        }

        List<ObjectId> ancestorsOfStartCommits = new LinkedList<>();

        for(var c: walk) {
            for(var p : c.getParents()) {
                if(ancestorsOfStartCommits.contains(p.getId())) {
                    return Optional.of(new JGitCommit(p, repo));
                } else {
                    ancestorsOfStartCommits.add(p);
                }
            }
        }

        return Optional.empty();
    }


    @Override
    public Optional<ICommit> getForkPoint(IBranch parentBranch) throws GitException{
        Collection<ReflogEntry> reflog;
        try {
            reflog = repo.getJgitGit().reflog().setRef(parentBranch.getFullName()).call();
        } catch (Exception e) {
            throw new JGitException(e);
        }

        RevWalk walk = new RevWalk(repo.getJgitRepo());
        walk.sort(RevSort.TOPO);
        RevCommit commit = getPointedRevCommit();
        try {
            walk.markStart(commit);
        } catch (Exception e) {
            throw new JGitException(e);
        }

        ReflogEntry[] refEntries = reflog.toArray(new ReflogEntry[0]);

        for(var curBranchCommit : walk) {
            for(var parentBranchReflogEntry : refEntries) {
                if(curBranchCommit.getId().equals(parentBranchReflogEntry.getNewId())) {
                    return Optional.of(new JGitCommit(curBranchCommit, repo));
                }
            }
        }

        return Optional.empty();
    }


    @Override
    public List<ICommit> getCommitsUntil(Optional<ICommit> upToCommit) throws GitException {
        RevWalk walk = new RevWalk(repo.getJgitRepo());
        walk.sort(RevSort.TOPO);
        RevCommit commit = getPointedRevCommit();
        try {
            walk.markStart(commit);
        } catch (Exception e) {
            throw new JGitException(e);
        }

        var list = new LinkedList<ICommit>();

        for(var c : walk) {
            if(upToCommit.isPresent() && c.getId().getName().equals(upToCommit.get().getHash().getHashString()))
                break;

            list.add(new JGitCommit(c, repo));
        }

        return list;
    }


    @Override
    public boolean isItAtBeginOfHistory() throws JGitException {
        Collection<ReflogEntry> rf;
        try {
            rf = repo.getJgitGit().reflog().setRef(getFullName()).call();
        } catch (GitAPIException e) {
            throw new JGitException(e);
        }

        var rfit = rf.iterator();

        if(!rfit.hasNext())
            return true;

        return rfit.next().getOldId().equals(ObjectId.zeroId());
    }

}
