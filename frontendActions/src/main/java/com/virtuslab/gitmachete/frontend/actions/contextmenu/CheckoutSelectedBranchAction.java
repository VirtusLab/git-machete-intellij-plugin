package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.common.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.actions.common.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.actions.common.IExpectsKeySelectedVcsRepository;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public class CheckoutSelectedBranchAction extends GitMacheteRepositoryReadyAction
    implements
      IExpectsKeyProject,
      IExpectsKeySelectedBranchName,
      IExpectsKeySelectedVcsRepository {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

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

    Option<String> currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);

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
      LOG.warn("Skipping the action because selected branch is undefined");
      return;
    }

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedVcsRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      LOG.debug(() -> "Queuing '${selectedBranchName.get()}' branch checkout background task");
      new Task.Backgroundable(project, "Checking out") {
        @Override
        public void run(ProgressIndicator indicator) {
          doCheckout(selectedBranchName.get(), gitRepository.get(), project, indicator);
        }
        // TODO (#95): on success, refresh only indication of the current branch
      }.queue();
    } else {
      LOG.warn("Skipping the action because no VCS repository is selected");
    }
  }

  public static void doCheckout(String branchNameToCheckout, GitRepository gitRepository, Project project,
      ProgressIndicator indicator) {
    LOG.info("Checking out branch '${branchNameToCheckout}'");
    GitBranchUiHandlerImpl uiHandler = new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator);
    new GitBranchWorker(project, Git.getInstance(), uiHandler)
        .checkout(branchNameToCheckout, /* detach */ false, java.util.List.of(gitRepository));
  }
}
