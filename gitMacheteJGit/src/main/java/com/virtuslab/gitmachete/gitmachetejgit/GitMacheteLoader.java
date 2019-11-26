package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.gitcore.gitcoreapi.*;
import com.virtuslab.gitmachete.gitmacheteapi.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;

public class GitMacheteLoader {
    @Inject
    private GitMacheteRepositoryFactory gitMacheteRepositoryFactory;
    @Inject
    private GitCoreRepositoryFactory gitCoreRepositoryFactory;

    private Path pathToRepoRoot;
    private Path pathToMacheteFile;
    private IRepository repo;

    private Character indentType = null;
    private int levelWidth = 0;

    @Inject
    public GitMacheteLoader(@Assisted Path pathToRepoRoot) {
        this.pathToRepoRoot = pathToRepoRoot;
    }

    public Repository getRepository() throws GitMacheteException {
        this.repo = gitCoreRepositoryFactory.create(pathToRepoRoot);
        this.pathToMacheteFile = this.repo.getGitFolderPath().resolve("machete");


        List<String> lines;
        try {
            lines = Files.readAllLines(pathToMacheteFile);
        } catch (IOException e) {
            throw new GitMacheteException(MessageFormat.format("Error while loading machete file ({0})", pathToMacheteFile.toAbsolutePath().toString()), e);
        }

        lines.removeIf(this::isEmptyLine);

        if(lines.size() < 1)
            return gitMacheteRepositoryFactory.create(this.repo, Optional.empty());

        if(getIndent(lines.get(0)) > 0)
            throw new MacheteFileParseException(MessageFormat.format("The initial line of machete file ({0}) cannot be indented", pathToMacheteFile.toAbsolutePath().toString()));


        int currentLevel = 0;
        Map<Integer, GitMacheteBranch> macheteBranchesLevelsMap = new HashMap<>();
        Map<Integer, ILocalBranch> coreBranchesLevelsMap = new HashMap<>();
        Repository repo = gitMacheteRepositoryFactory.create(this.repo, Optional.empty());
        for(var line : lines) {
            int level = getLevel(getIndent(line));

            if(level-currentLevel > 1)
                throw new MacheteFileParseException(MessageFormat.format("One of branches in machete file ({0}) has incorrect level in relation to its parent branch", pathToMacheteFile.toAbsolutePath().toString()));


            String branchName = line.trim();
            ILocalBranch coreLocalBranch;
            try {
                coreLocalBranch = this.repo.getLocalBranch(branchName);     //Checking if local branch of this name really exists in this repository
            } catch (GitException e) {
                throw new GitImplementationException(e);
            }


            var branch = new GitMacheteBranch(branchName);

            macheteBranchesLevelsMap.put(level, branch);
            coreBranchesLevelsMap.put(level, coreLocalBranch);

            try {
                branch.syncToOriginStatus = getSyncToOriginByTrackingStatus(coreLocalBranch.getTrackingStatus());

                if (level == 0) {
                    branch.commits = List.of();
                    branch.syncToParentStatus = SyncToParentStatus.InSync;
                    repo.addRootBranch(branch);
                } else {
                    branch.commits = getCommitsBelongingSpecificallyToBranch(coreLocalBranch, coreBranchesLevelsMap.get(level - 1));
                    branch.syncToParentStatus = getSyncToParentStatus(coreLocalBranch, coreBranchesLevelsMap.get(level-1));
                    macheteBranchesLevelsMap.get(level - 1).childBranches.add(branch);
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

    private int getIndent(String l) {
        int indent = 0;
        for(int i=0; i<l.length(); i++) {
            if (indentType == null) {
                if (l.charAt(i) != ' ' && l.charAt(i) != '\t') {
                    break;
                } else {
                    indent++;
                    indentType = l.charAt(i);
                }
            } else {
                if (l.charAt(i) == indentType)
                    indent++;
                else
                    break;
            }
        }

        return indent;
    }

    private int getLevel(int indent) throws MacheteFileParseException {
        if(levelWidth == 0 && indent > 0) {
            levelWidth = indent;
            return 1;
        }
        else if (indent == 0) {
            return 0;
        }

        if(indent%levelWidth != 0)
            throw new MacheteFileParseException(MessageFormat.format("Levels of indentations are not matching in machete file ({0})", pathToMacheteFile.toAbsolutePath().toString()));

        return indent/levelWidth;
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

    private List<Commit> getCommitsBelongingSpecificallyToBranch(ILocalBranch childBranch, ILocalBranch parentBranch) throws GitException{
        Optional<ICommit> forkPoint = childBranch.getForkPoint(parentBranch);
        if(forkPoint.isEmpty())
            return List.of();

        return translateICommitsToCommits(childBranch.getCommitsUntil(forkPoint));
    }


    private SyncToParentStatus getSyncToParentStatus(ILocalBranch childBranch, ILocalBranch parentBranch) throws GitException {
        if(childBranch.getPointedCommit().equals(parentBranch.getPointedCommit())) {
            if(childBranch.isItAtBeginOfHistory())
                return SyncToParentStatus.InSync;
            else
                return SyncToParentStatus.Merged;
        } else {
            Optional<ICommit> forkPoint = childBranch.getForkPoint(parentBranch);
            boolean isParentAncestorOfChild = parentBranch.getPointedCommit().isAncestorOf(childBranch.getPointedCommit());

            if(isParentAncestorOfChild) {
                if(forkPoint.isEmpty() || !forkPoint.get().equals(parentBranch.getPointedCommit()))
                    return SyncToParentStatus.NotADirectDescendant;
                else
                    return SyncToParentStatus.InSync;
            } else {
                boolean isChildAncestorOfParent = childBranch.getPointedCommit().isAncestorOf(parentBranch.getPointedCommit());

                if(isChildAncestorOfParent)
                    return SyncToParentStatus.Merged;
                else
                    return SyncToParentStatus.OutOfSync;
            }
        }
    }

}
