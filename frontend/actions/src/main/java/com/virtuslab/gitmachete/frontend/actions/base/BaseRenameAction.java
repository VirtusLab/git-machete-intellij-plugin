package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.branch.GitBranchOperationType;
import git4idea.branch.GitNewBranchDialog;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.RenameBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public abstract class BaseRenameAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName != null
        ? getManagedBranchByName(anActionEvent, branchName)
        : null;

    if (branch == null) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.undefined.machete-branch")
          .fmt("Rename", getQuotedStringOrCurrent(branchName)));
    } else {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseRenameAction.description").fmt(branch.getName()));
    }
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName != null ? getManagedBranchByName(anActionEvent, branchName) : null;
    val branchLayout = getBranchLayout(anActionEvent);
    val graphTable = getGraphTable(anActionEvent);

    if (gitRepository == null || branchName == null || branchLayout == null || branch == null || graphTable == null) {
      return;
    }

    rename(gitRepository, graphTable, branch, branchLayout);
  }

  @ContinuesInBackground
  @UIEffect
  private void rename(GitRepository gitRepository, BaseEnhancedGraphTable graphTable, IManagedBranchSnapshot branch,
      BranchLayout branchLayout) {
    val project = gitRepository.getProject();

    val gitNewBranchDialog = new GitNewBranchDialog(project, Collections.singletonList(gitRepository),
        getString("action.GitMachete.BaseRenameAction.description").fmt(branch.getName()),
        branch.getName(),
        /* showCheckOutOption */ false,
        /* showResetOption */ false,
        /* showSetTrackingOption */ false,
        /* localConflictsAllowed */ false,
        GitBranchOperationType.RENAME);

    val options = gitNewBranchDialog.showAndGetOptions();

    if (options != null) {
      new RenameBackgroundable(gitRepository,
          graphTable,
          branchLayout,
          branch.getName(),
          options.getName())
              .queue();

    }
  }
}
