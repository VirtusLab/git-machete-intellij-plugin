package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import java.util.List;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 * </ul>
 */
public class CheckoutSelectedBranchAction extends DumbAwareAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var selectedBranchName = ActionUtils.getSelectedBranchName(anActionEvent);
    // It's very unlikely that selectedBranchName is empty at this point since it's assigned directly before invoking this
    // action in GitMacheteGraphTable.GitMacheteGraphTableMouseAdapter.mouseClicked; still, it's better to be safe.
    if (selectedBranchName.isDefined()) {
      Option<String> currentBranchName = ActionUtils.getCurrentBranchNameIfManaged(anActionEvent);

      if (currentBranchName.isDefined() && currentBranchName.get().equals(selectedBranchName.get())) {
        anActionEvent.getPresentation().setEnabled(false);
        anActionEvent.getPresentation().setDescription("Branch '${selectedBranchName.get()}' is currently checked out");
      } else {
        anActionEvent.getPresentation().setDescription("Checkout branch '${selectedBranchName.get()}'");
      }
    } else {
      anActionEvent.getPresentation().setEnabled(false);
      anActionEvent.getPresentation().setDescription("Checkout disabled due to undefined selected branch");
    }
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    var selectedBranchName = ActionUtils.getSelectedBranchName(anActionEvent);
    if (selectedBranchName.isEmpty()) {
      LOG.warn("Skipping the action because selected branch is undefined");
      return;
    }

    Project project = ActionUtils.getProject(anActionEvent);
    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(anActionEvent);

    if (selectedVcsRepository.isDefined()) {
      LOG.debug(() -> "Queuing '${selectedBranchName.get()}' branch checkout background task");
      new Task.Backgroundable(project, "Checking out") {
        @Override
        public void run(ProgressIndicator indicator) {
          LOG.info("Checking out branch '${selectedBranchName.get()}'");
          GitBranchUiHandlerImpl uiHandler = new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator);
          new GitBranchWorker(project, Git.getInstance(), uiHandler)
              .checkout(selectedBranchName.get(), /* detach */ false, List.of(selectedVcsRepository.get()));
        }
        // TODO (#95): on success, refresh only indication of the current branch
      }.queue();
    } else {
      LOG.warn("Skipping the action because no VCS repository is selected");
    }
  }
}
