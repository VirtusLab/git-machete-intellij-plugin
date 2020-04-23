package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.keys.ActionIDs.ACTION_REFRESH;

import java.io.IOException;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.impl.BranchLayoutFileSaver;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.keys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_FILE_PATH}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class SlideOutCurrentBranchAction extends GitMacheteRepositoryReadyAction {
  private static final Logger LOG = Logger.getInstance(SlideOutCurrentBranchAction.class);

  private static final String ACTION_TEXT = "Slide Out Current Branch";
  private static final String ACTION_DESCRIPTION = "Slide out current branch";

  public SlideOutCurrentBranchAction() {
    super(ACTION_TEXT, ACTION_DESCRIPTION, AllIcons.Actions.GC);
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (presentation.isEnabledAndVisible()) {
      IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);

      var currentBranchOption = gitMacheteRepository.getCurrentBranchIfManaged();

      if (currentBranchOption.isEmpty()) {
        presentation.setDescription("Current revision is not a branch managed by Git Machete");
        presentation.setEnabled(false);

      } else if (currentBranchOption.get().isRootBranch()) {
        presentation.setDescription("Can't slide out git machete root branch '${currentBranchOption.get().getName()}'");
        presentation.setEnabled(false);

      } else {
        presentation.setDescription("Slide out '${currentBranchOption.get().getName()}'");
      }
    }
  }

  /**
   * Assumption to the following code is that the result of {@link IGitMacheteRepository#getCurrentBranchIfManaged}
   * is present and it is not a root branch because if it was not the user wouldn't be able to perform action in the first place
   */
  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    Project project = anActionEvent.getProject();
    assert project != null;

    Option<BaseGitMacheteBranch> currentBranch = getMacheteRepository(anActionEvent).getCurrentBranchIfManaged();
    assert currentBranch.isDefined();

    var branchLayout = anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT);
    try {
      var branchName = currentBranch.get().getName();
      var newBranchLayout = branchLayout.slideOut(branchName);
      var macheteFilePath = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_FILE_PATH);
      var branchLayoutFileSaver = new BranchLayoutFileSaver(macheteFilePath);

      try {
        branchLayoutFileSaver.save(newBranchLayout, /* backupOldFile */ true);
        ActionManager.getInstance().getAction(ACTION_REFRESH).actionPerformed(anActionEvent);
        VcsNotifier.getInstance(project).notifyInfo("Branch ${branchName} slid out");
      } catch (IOException e) {
        LOG.error("Failed to save machete file", e);
      }
    } catch (BranchLayoutException e) {
      String message = e.getMessage();
      VcsNotifier.getInstance(project).notifyError("Slide out failed", message == null ? "" : message);
    }
  }
}
