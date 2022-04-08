package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.logger.IEnhancedLambdaLogger;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class CheckoutSelectedBranchAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeySelectedBranchName {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val selectedBranchName = getSelectedBranchName(anActionEvent);
    // It's very unlikely that selectedBranchName is empty at this point since it's assigned directly before invoking this
    // action in GitMacheteGraphTable.GitMacheteGraphTableMouseAdapter.mouseClicked; still, it's better to be safe.
    if (selectedBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getString("action.GitMachete.CheckoutSelectedBranchAction.undefined.branch-name"));
      return;
    }

    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);

    if (currentBranchName.isDefined() && currentBranchName.get().equals(selectedBranchName.get())) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.CheckoutSelectedBranchAction.description.disabled.currently-checked-out"),
              selectedBranchName.get()));

    } else {
      presentation.setDescription(
          format(getString("action.GitMachete.CheckoutSelectedBranchAction.description.precise"), selectedBranchName.get()));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val selectedBranchName = getSelectedBranchName(anActionEvent);
    if (selectedBranchName.isEmpty()) {
      return;
    }

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      log().debug(() -> "Queuing '${selectedBranchName.get()}' branch checkout background task");
      new Task.Backgroundable(project, getString("action.GitMachete.CheckoutSelectedBranchAction.task-title")) {
        @Override
        @UIThreadUnsafe
        public void run(ProgressIndicator indicator) {
          doCheckout(project, indicator, selectedBranchName.get(), gitRepository.get());
        }
        // TODO (#95): on success, refresh only indication of the current branch
      }.queue();
    }
  }

  @UIThreadUnsafe
  public static void doCheckout(Project project, ProgressIndicator indicator, String branchToCheckoutName,
      GitRepository gitRepository) {
    // TODO (#772): switch to constructor that does not take git once we no longer support 2021.2
    GitBranchUiHandlerImpl uiHandler = new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator);
    new GitBranchWorker(project, Git.getInstance(), uiHandler)
        .checkout(branchToCheckoutName, /* detach */ false, Collections.singletonList(gitRepository));
  }
}
