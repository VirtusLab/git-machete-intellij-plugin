package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.actions.common.BranchCreationUtils.waitForCreationOfLocalBranch;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.runWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.branch.GitBranchOperationType;
import git4idea.branch.GitBrancher;
import git4idea.branch.GitNewBranchDialog;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
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
      val gitBrancher = GitBrancher.getInstance(project);
      Runnable renameRunnable = () -> gitBrancher.renameBranch(branch.getName(), options.getName(),
          Collections.singletonList(gitRepository));

      Path macheteFilePath = gitRepository.getMacheteFilePath();

      val newBranchLayout = branchLayout.rename(branch.getName(), options.getName());
      val branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);

      graphTable.disableEnqueuingUpdates();
      renameRunnable.run();

      runWriteActionOnUIThread(() -> {
        MacheteFileWriter.writeBranchLayout(
            macheteFilePath,
            branchLayoutWriter,
            newBranchLayout,
            /* backupOldLayout */ true,
            /* requestor */ this);

      });

      // `renameRunnable` may perform some sneakily-asynchronous operations (e.g. renameBranch).
      // The high-level method used within the runnable does not allow us to schedule the tasks after them.
      // (Stepping deeper is not an option since we would lose some important logic or become very dependent on the internals of git4idea).
      // Hence, we wait for the creation of the branch (with exponential backoff).
      waitForCreationOfLocalBranch(gitRepository, options.getName());
      graphTable.enableEnqueuingUpdates();
    }
  }
}
