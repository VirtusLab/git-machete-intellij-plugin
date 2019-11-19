package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitcore.*;
import com.virtuslab.gitmachete.gitcorejgit.*;
import com.virtuslab.gitmachete.gitmacheteapi.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;

public class GitMacheteLoader {
    private Path pathToRepoRoot;
    private Path pathTogGtFolder;
    private Path pathToMacheteFile;

    private boolean identTypeDetected = false;
    private Function<Character, Boolean> identDetectionFunction = c -> {
        if(c.equals(' ') || c.equals('\t')) {
            identDetectionFunction = a -> a.equals(c);
            return true;
        }
        else
            return false;
    };
    private int levelWidth = 0;

    public GitMacheteLoader(Path pathToRepoRoot) {
        this.pathToRepoRoot = pathToRepoRoot;
        this.pathTogGtFolder = pathToRepoRoot.resolve(".git");
        this.pathToMacheteFile = this.pathTogGtFolder.resolve("machete");
    }

    public Repository getRepository() throws IOException, IdentsMismatchException, LevelsMismatchException, GitImplementationException {
        List<String> lines = Files.readAllLines(pathToMacheteFile);

        lines.removeIf(this::isEmptyLine);

        if(lines.size() < 1)
            return new GitMacheteRepository(pathToRepoRoot);

        int initialIdentation = getIdent(lines.get(0));

        IRepository jgr = new JGitRepository(pathTogGtFolder.toString());


        int currentLevel = 0;
        Map<Integer, GitMacheteBranch> branchesLevelsMap = new HashMap<>();
        Map<Integer, ILocalBranch> jgitBranchesLevelsMap = new HashMap<>();
        GitMacheteRepository repo = new GitMacheteRepository(pathToRepoRoot);
        for(var l : lines) {
            int ident = getIdent(l) - initialIdentation;

            if(ident < 0)
                throw new IdentsMismatchException(MessageFormat.format("Branch \"{0}\" in machete file has identation below entry level of this file", l.trim()));

            int level = getLevel(ident);

            if(level-currentLevel > 1)
                throw new LevelsMismatchException("One of branches in machete file has incorrect level in relation to is's parent branch");


            String branchName = l.trim();
            ILocalBranch jglb;
            try {
                jglb = jgr.getLocalBranch(branchName);     //Checking if local branch of this name really exists in this repository
            } catch (GitException e) {
                throw new GitImplementationException(e);
            }


            var branch = new GitMacheteBranch(branchName);

            branchesLevelsMap.put(level, branch);
            jgitBranchesLevelsMap.put(level, jglb);

            try {
                branch.syncToOriginStatus = getSyncToOriginByTrackingStatus(jglb.getTrackingStatus());

                if (level == 0) {
                    branch.commits = translateICommitsToCommits(jglb.getBelongingCommits(Optional.empty()));
                    branch.syncToParentStatus = SyncToParentStatus.InSync;
                    repo.branches.add(branch);
                } else {
                    branch.commits = getCommitsBelongingSpecificallyToBranch(jglb, jgitBranchesLevelsMap.get(level - 1));
                    branch.syncToParentStatus = getSyncToParentStatus(jglb, jgitBranchesLevelsMap.get(level-1));
                    branchesLevelsMap.get(level - 1).branches.add(branch);
                }
            } catch (GitException e) {
                throw new GitImplementationException(e);
            }

            currentLevel = level;
        }

        return repo;
    }


    private boolean isEmptyLine(String l) {
        return l.trim().length() < 1;
    }

    private int getIdent(String l) {
        int ident = 0;
        for(int i=0; i<l.length(); i++) {
            if (identDetectionFunction.apply(l.charAt(i)))
                ident++;
            else
                break;
        }

        return ident;
    }

    private int getLevel(int realIdent) throws IdentsMismatchException {   //Returns -1 if is incorrect
        if(levelWidth == 0 && realIdent > 0) {
            levelWidth = realIdent;
            return 1;
        }
        else if (realIdent == 0) {
            return 0;
        }

        if(realIdent%levelWidth != 0)
            throw new IdentsMismatchException("Levels of identations are mismatch");

        return realIdent/levelWidth;
    }


    private SyncToOriginStatus getSyncToOriginByTrackingStatus(Optional<IBranchTrackingStatus> ts) {
        if(ts.isEmpty())
            return SyncToOriginStatus.Untracked;

        if(ts.get().getAhead() > 0 && ts.get().getBehind() > 0)
            return SyncToOriginStatus.Diverged;
        else if(ts.get().getAhead() > 0)
            return SyncToOriginStatus.Ahead;
        else if (ts.get().getBehind() > 0)
            return SyncToOriginStatus.Behind;
        else
            return SyncToOriginStatus.InSync;
    }


    private List<Commit> translateICommitsToCommits(List<ICommit> list) throws GitException {
        var l = new LinkedList<Commit>();

        for(var c : list) {
            l.add(new GitMacheteCommit(c.getMessage()));
        }

        return l;
    }

    private List<Commit> getCommitsBelongingSpecificallyToBranch(ILocalBranch branch, ILocalBranch parentBranch) throws GitException{
        Optional<ICommit> forkPoint = branch.getForkPoint(parentBranch);
        if(forkPoint.isEmpty())
            return List.of();

        return translateICommitsToCommits(branch.getBelongingCommits(forkPoint));
    }


    private SyncToParentStatus getSyncToParentStatus(ILocalBranch branch, ILocalBranch parentBranch) throws GitException {
        if(branch.getPointedCommit().equals(parentBranch.getPointedCommit())) {
            if(branch.isItAtBeginOfHistory())
                return SyncToParentStatus.InSync;
            else
                return SyncToParentStatus.Merged;
        } else {
            Optional<ICommit> forkPoint = branch.getForkPoint(parentBranch);
            boolean isParentAncestorOfChild = parentBranch.getPointedCommit().isAncestorOf(branch.getPointedCommit());

            if(isParentAncestorOfChild) {
                if(forkPoint.isEmpty() || !forkPoint.get().equals(parentBranch.getPointedCommit()))
                    return SyncToParentStatus.NotADirectDescendant;
                else
                    return SyncToParentStatus.InSync;
            } else {
                boolean isChildAncestorOfParent = branch.getPointedCommit().isAncestorOf(parentBranch.getPointedCommit());

                if(isChildAncestorOfParent)
                    return SyncToParentStatus.Merged;
                else
                    return SyncToParentStatus.OutOfSync;
            }
        }
    }

}
