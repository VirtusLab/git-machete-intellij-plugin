package com.virtuslab.gitmachete.gitmachetejgit;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.gitcore.gitcoreapi.*;
import com.virtuslab.gitmachete.gitmacheteapi.Branch;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.Repository;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;

@Getter
public class GitMacheteRepository implements Repository {
    @Getter
    private Optional<String> repositoryName;
    @Getter(AccessLevel.NONE)
    private IRepository repo;
    List<Branch> rootBranches = new LinkedList<>();

    @Inject
    @Getter(AccessLevel.NONE)
    private GitMacheteRepositoryFactory gitMacheteRepositoryFactory;

    @Inject
    @Getter(AccessLevel.NONE)
    private GitCoreRepositoryFactory gitCoreRepositoryFactory;

    @Inject
    public GitMacheteRepository(@Assisted IRepository repo, @Assisted Optional<String> name) {
        this.repo = repo;
        this.repositoryName = name;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for(var b : rootBranches) {
            printBranch(b, 0, sb);
        }

        return sb.toString();
    }

    private void printBranch(Branch branch, int level, StringBuilder sb) {
        sb.append("\t".repeat(level));
        sb.append(branch.getName());
        sb.append(" - (Remote: ");
        sb.append(branch.getSyncToOriginStatus());
        sb.append("; Parent: ");
        sb.append(branch.getSyncToParentStatus());
        sb.append(")");
        for(var c : branch.getCommits()) {
            sb.append("; ");
            sb.append(c.getMessage().split("\n", 2)[0]);
        }
        sb.append(System.lineSeparator());

        for(var b : branch.getBranches()) {
            printBranch(b, level+1, sb);
        }
    }


    @Override
    public Optional<Branch> getCurrentBranch() throws GitMacheteException {
        Optional<ILocalBranch> branch;
        try {
            branch = repo.getCurrentBranch();
        } catch (GitException e) {
            throw new GitMacheteJGitException("Error occurred while getting current branch object", e);
        }

        if(branch.isEmpty())
            return Optional.empty();
        else
            try {
                return Optional.of(new GitMacheteBranch(branch.get().getName()));
            } catch (GitException e) {
                throw new GitMacheteJGitException("Error occurred while getting current branch name", e);
            }
    }

    @Override
    public void addRootBranch(Branch branch) {
        rootBranches.add(branch);
    }


    @Override
    public Map<String, Repository> getSubmoduleRepositories() throws GitMacheteException {
        Map<String, Repository> submodules = new HashMap<String, Repository>();

        Map<String, ISubmoduleEntry> subs;

        try {
            subs = this.repo.getSubmodules();
        } catch (GitException e) {
            throw new GitMacheteJGitException("Error while getting submodules", e);
        }

        for(var e : subs.entrySet()) {
            Repository r = gitMacheteRepositoryFactory.create(gitCoreRepositoryFactory.create(e.getValue().getPath()), Optional.of(e.getValue().getName()));
            submodules.put(e.getKey(), r);
        }

        return submodules;
    }
}
