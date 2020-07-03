package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class CheckoutSelectedBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeyProject,
      IExpectsKeySelectedBranchName {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    if (!canBeUpdated(anActionEvent)) {
      return;
    }

    var presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var selectedBranchName = getSelectedBranchName(anActionEvent);
    // It's very unlikely that selectedBranchName is empty at this point since it's assigned directly before invoking this
    // action in GitMacheteGraphTable.GitMacheteGraphTableMouseAdapter.mouseClicked; still, it's better to be safe.
    if (selectedBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription("Checkout disabled due to undefined selected branch");
      return;
    }

    var currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);

    if (currentBranchName.isDefined() && currentBranchName.get().equals(selectedBranchName.get())) {
      presentation.setEnabled(false);
      presentation.setDescription("Branch '${selectedBranchName.get()}' is currently checked out");

    } else {
      presentation.setDescription("Checkout branch '${selectedBranchName.get()}'");
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    var selectedBranchName = getSelectedBranchName(anActionEvent);
    if (selectedBranchName.isEmpty()) {
      return;
    }

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      log().debug(() -> "Queuing '${selectedBranchName.get()}' branch checkout background task");
      new Task.Backgroundable(project, "Checking out") {
        @Override
        public void run(ProgressIndicator indicator) {
          doCheckout(selectedBranchName.get(), gitRepository.get(), project, indicator);
        }
        // TODO (#95): on success, refresh only indication of the current branch
      }.queue();
    }
  }

  public static void doCheckout(String branchNameToCheckout, GitRepository gitRepository, Project project,
      ProgressIndicator indicator) {
    GitBranchUiHandlerImpl uiHandler = new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator);
    new GitBranchWorker(project, Git.getInstance(), uiHandler)
        .checkout(branchNameToCheckout, /* detach */ false, java.util.List.of(gitRepository));
  }
}
