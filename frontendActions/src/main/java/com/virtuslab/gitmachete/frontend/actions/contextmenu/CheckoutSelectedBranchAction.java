package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentBranchNameIfManaged;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getProject;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedBranchName;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.EnhancedLambdaLoggerFactory;
import com.virtuslab.logger.IEnhancedLambdaLogger;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class CheckoutSelectedBranchAction extends GitMacheteRepositoryReadyAction {
  private static final IEnhancedLambdaLogger LOG = EnhancedLambdaLoggerFactory.create();

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
          LOG.info("Checking out branch '${selectedBranchName.get()}'");
          GitBranchUiHandlerImpl uiHandler = new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator);
          new GitBranchWorker(project, Git.getInstance(), uiHandler)
              .checkout(selectedBranchName.get(), /* detach */ false, java.util.List.of(gitRepository.get()));
        }
        // TODO (#95): on success, refresh only indication of the current branch
      }.queue();
    } else {
      LOG.warn("Skipping the action because no VCS repository is selected");
    }
  }
}
