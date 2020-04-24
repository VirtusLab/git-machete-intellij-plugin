package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getPresentIdeaRepository;

import java.util.List;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.GuiUtils;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.guieffect.qual.UIEffect;

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
public class CheckOutBranchAction extends AnAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  public CheckOutBranchAction() {}

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    String selectedBranchName = anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME);
    if (selectedBranchName == null) {
      LOG.error("Branch to check out was not given");
      GuiUtils.invokeLaterIfNeeded(
          () -> Messages.showErrorDialog("Internal error occurred during check out: Branch to check out was not given",
              "Something Went Wrong..."),
          ModalityState.NON_MODAL);
      return;
    }

    Project project = anActionEvent.getProject();
    assert project != null : "Can't get project from anActionEvent variable";
    GitRepository repository = getPresentIdeaRepository(anActionEvent);

    LOG.debug(() -> "Queuing '${selectedBranchName}' branch checkout background task");
    new Task.Backgroundable(project, "Checking out") {
      @Override
      public void run(ProgressIndicator indicator) {
        LOG.info(() -> "Checking out branch '${selectedBranchName}'");
        new GitBranchWorker(project, Git.getInstance(),
            new GitBranchUiHandlerImpl(project, Git.getInstance(), indicator))
                .checkout(selectedBranchName, /* detach */ false, List.of(repository));
      }
      // TODO (#95): on success, refresh only indication of the current branch
    }.queue();
  }
}
