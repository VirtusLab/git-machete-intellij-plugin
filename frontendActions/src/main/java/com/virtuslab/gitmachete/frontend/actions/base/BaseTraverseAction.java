package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;
import git4idea.repo.GitRepository;
import java.util.concurrent.ExecutionException;
import lombok.CustomLog;

import static com.virtuslab.gitmachete.frontend.actions.base.BaseRebaseBranchOntoParentAction.doRebase;

@CustomLog
public abstract class BaseTraverseAction extends BaseGitMacheteRepositoryReadyAction
implements IBranchNameProvider,
        IExpectsKeyProject,
        IExpectsKeyGitMacheteRepository {
    @Override
    public IEnhancedLambdaLogger log() {
        return LOG;
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        log().debug("Performing");

        var project = getProject(anActionEvent);
        var gitRepository = getSelectedGitRepository(anActionEvent);
        var branchName = getNameOfBranchUnderAction(anActionEvent);
        var gitMacheteRepository = getGitMacheteRepositoryWithLoggingOnEmpty(anActionEvent);

        if (branchName.isDefined() && gitRepository.isDefined() && gitMacheteRepository.isDefined()) {
            doTraverse(project, gitRepository.get(), gitMacheteRepository.get(), branchName.get());
        }
    }

    public void doTraverse(Project project, GitRepository gitRepository, IGitMacheteRepository gitMacheteRepository, String branchName) {

    }

    private void processBranch(Project project, GitRepository gitRepository, IGitMacheteRepository gitMacheteRepository, IGitMacheteNonRootBranch branch) {
        if (branch.getSyncToParentStatus() == SyncToParentStatus.OutOfSync) {
            var task = doRebase(project, gitRepository, gitMacheteRepository, branch);
            try {
                task.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        for (var downstreamBranch : branch.getDownstreamBranches()) {
            processBranch(project, gitRepository, gitMacheteRepository, downstreamBranch);
        }
    }
}
