package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.runWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
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
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
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
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val currentBranchName = getNameOfBranchUnderAction(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);
    val branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);

    if (gitRepository == null || currentBranchName == null || branchLayout == null) {
      return;
    }

    rename(project, gitRepository, currentBranchName, branchLayout, branchLayoutWriter);
  }

  @ContinuesInBackground
  @UIEffect
  private void rename(Project project,
      GitRepository gitRepository,
      String currentBranchName, BranchLayout branchLayout, IBranchLayoutWriter branchLayoutWriter) {
    val gitNewBranchDialog = new GitNewBranchDialog(project, Collections.singletonList(gitRepository),
        getString("action.GitMachete.BaseRenameAction.description").fmt(currentBranchName),
        currentBranchName,
        /* showCheckOutOption */ false,
        /* showResetOption */ false,
        /* showSetTrackingOption */ false,
        /* localConflictsAllowed */ false,
        GitBranchOperationType.RENAME);

    val options = gitNewBranchDialog.showAndGetOptions();

    if (options != null) {
      Path macheteFilePath = gitRepository.getMacheteFilePath();
      val newBranchLayout = branchLayout.rename(currentBranchName, options.getName());

      runWriteActionOnUIThread(() -> {
        MacheteFileWriter.writeBranchLayout(
            macheteFilePath,
            branchLayoutWriter,
            newBranchLayout,
            /* backupOldLayout */ true,
            /* requestor */ this);

        GitBrancher.getInstance(project).renameBranch(currentBranchName, options.getName(),
            Collections.singletonList(gitRepository));
      });
    }
  }
}
