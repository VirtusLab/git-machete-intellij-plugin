package com.virtuslab.gitmachete.frontend.actions.common;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentBranchNameIfManaged;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteRepository;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;

import java.util.List;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BaseResetBranchToRemoteAction extends GitMacheteRepositoryReadyAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");
  private static final String VCS_LOGGER_TITLE = "Resetting";
  private static final String TASK_TITLE = "Resetting...";

  protected abstract Option<String> getBranchName(AnActionEvent anActionEvent);

  protected abstract String getActionType();

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    Option<String> branchNameOption = getBranchName(anActionEvent);

    if (branchNameOption.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Reset disabled due to undefined ${getActionType()} branch");
      return;
    }

    String branchName = branchNameOption.get();

    Option<SyncToRemoteStatus> syncToRemoteStatus = getGitMacheteRepository(anActionEvent)
        .flatMap(repo -> repo.getBranchByName(branchName))
        .map(BaseGitMacheteBranch::getSyncToRemoteStatus);

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Reset disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();

    if (relation != SyncToRemoteStatus.Relation.Untracked) {
      presentation.setDescription("Reset branch '${branchName} to its remote tracking branch");
    } else {
      presentation.setEnabled(false);
      presentation.setDescription("Reset disabled because ${getActionType()} branch is untracked");
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");

    Project project = getProject(anActionEvent);
    Option<GitRepository> selectedVcsRepository = getSelectedVcsRepository(anActionEvent);
    Option<String> branchName = getBranchName(anActionEvent);
    Option<IGitMacheteRepository> macheteRepository = getGitMacheteRepository(anActionEvent);

    if (branchName.isDefined()) {
      if (selectedVcsRepository.isDefined()) {
        if (macheteRepository.isDefined()) {
          doResetToRemoteWithKeep(project, selectedVcsRepository.get(), branchName.get(), macheteRepository.get(),
              anActionEvent);
        } else {
          LOG.error("Skipping the action because can't get Git Machete repository");
          VcsNotifier.getInstance(project).notifyError(VCS_LOGGER_TITLE,
              "Internal error occurred. For more information see IDE log file");
        }
      } else {
        LOG.warn("Skipping the action because no VCS repository is selected");
        VcsNotifier.getInstance(project).notifyWarning(VCS_LOGGER_TITLE,
            "Skipping the action because no VCS repository is selected");
      }
    } else {
      LOG.warn("Skipping the action because name of branch to reset is undefined");
      VcsNotifier.getInstance(project).notifyError(VCS_LOGGER_TITLE,
          "Internal error occurred. For more information see IDE log file");
    }
  }

  protected void doResetToRemoteWithKeep(Project project, GitRepository gitRepository, String branchName,
      IGitMacheteRepository macheteRepository, AnActionEvent anActionEvent) {

    new Task.Backgroundable(project, TASK_TITLE, /* canBeCancelled */ true) {

      @Override
      public void run(ProgressIndicator indicator) {
        LOG.debug(() -> "Resetting '${branchName}' branch");
        try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(project, TASK_TITLE)) {
          GitLineHandler resetHandler = new GitLineHandler(myProject, gitRepository.getRoot(), GitCommand.RESET);
          resetHandler.addParameters("--keep");

          Option<BaseGitMacheteBranch> branchOption = macheteRepository.getBranchByName(branchName);
          assert branchOption.isDefined() : "Can't get branch '${branchName}' from Git Machete repository";
          Option<IGitMacheteRemoteBranch> remoteTrackingBranchOption = branchOption.get().getRemoteTrackingBranch();
          if (remoteTrackingBranchOption.isDefined()) {
            resetHandler.addParameters(remoteTrackingBranchOption.get().getPointedCommit().getHash());
          } else {
            String message = "Branch '${branchName}' doesn't have remote tracking branch, so cannot be reset";
            LOG.warn(message);
            VcsNotifier.getInstance(project).notifyWarning(VCS_LOGGER_TITLE, message);
          }

          resetHandler.endOptions();

          // Check if branch to reset is not current branch - if isn't then checkout
          Option<String> currentBranchOption = getCurrentBranchNameIfManaged(anActionEvent);
          if (currentBranchOption.isEmpty() || !currentBranchOption.get().equals(branchName)) {
            // Checking out given branch
            GitBranchUiHandlerImpl uiHandler = new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator);
            new GitBranchWorker(project, Git.getInstance(), uiHandler)
                .checkout(branchName, /* detach */ false, List.of(gitRepository));

            // Check again if we are in branch to reset to be sure that checkout was successful
            // This time we are using git4idea because GitMacheteRepository is immutable and it would return previous branch
            GitLocalBranch nullableLocalBranch = gitRepository.getCurrentBranch();
            if (nullableLocalBranch == null || !nullableLocalBranch.getName().equals(branchName)) {
              LOG.error("Checkout to branch ${branchName} before reset was unsuccessful");
              VcsNotifier.getInstance(project).notifyError(VCS_LOGGER_TITLE,
                  "Error occurred during checking out to branch that was to be reset! Operation aborted.");
              return;
            }
          }

          GitCommandResult result = Git.getInstance().runCommand(resetHandler);
          if (!result.success()) {
            LOG.error(result.getErrorOutputAsJoinedString());
            VcsNotifier.getInstance(project).notifyError(VCS_LOGGER_TITLE, result.getErrorOutputAsHtmlString());
          }
          GitRepositoryManager.getInstance(project).updateRepository(gitRepository.getRoot());
          // If `changes` is null the whole root will be refreshed
          GitUtil.refreshVfs(gitRepository.getRoot(), /* changes */ null);
        }

        // If we are here this mean that all went good
        VcsNotifier.getInstance(project).notifySuccess("Branch '${branchName}' reset to remote");
        LOG.debug(() -> "Branch '${branchName}' reset to its remote tracking branch");
      }
    }.queue();
  }
}
