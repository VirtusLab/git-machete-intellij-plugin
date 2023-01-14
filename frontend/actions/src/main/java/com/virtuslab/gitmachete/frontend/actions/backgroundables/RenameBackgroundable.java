package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.common.BranchCreationUtils.waitForCreationOfLocalBranch;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, Objects.class})
public class RenameBackgroundable extends SideEffectingBackgroundable {

  protected final Project project;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;
  private final BranchLayout branchLayout;
  private final String currentBranchName;
  private final String newBranchName;

  public RenameBackgroundable(
      GitRepository gitRepository,
      BaseEnhancedGraphTable graphTable, BranchLayout branchLayout,
      String currentBranchName,
      String newBranchName) {
    super(gitRepository.getProject(), getString("action.GitMachete.RenameBackgroundable.task-title"), "rename");
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;
    this.branchLayout = branchLayout;
    this.currentBranchName = currentBranchName;
    this.newBranchName = newBranchName;
  }

  @Override
  @UIThreadUnsafe
  @ContinuesInBackground
  public void doRun(ProgressIndicator indicator) {
    graphTable.disableEnqueuingUpdates();

    Path macheteFilePath = gitRepository.getMacheteFilePath();

    val newBranchLayout = branchLayout.rename(currentBranchName, newBranchName);
    val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);
    val gitBrancher = GitBrancher.getInstance(project);

    gitBrancher.renameBranch(currentBranchName, newBranchName, Collections.singletonList(gitRepository));

    blockingRunWriteActionOnUIThread(() -> MacheteFileWriter.writeBranchLayout(
        macheteFilePath,
        branchLayoutWriter,
        newBranchLayout,
        /* backupOldLayout */ true,
        /* requestor */ this));

    // `gitBrancher.renameBranch` continues asynchronously and doesn't allow for passing a callback to execute once complete.
    // (Stepping deeper is not an option since we would lose some important logic or become very dependent on the internals of git4idea).
    // Hence, we wait for the creation of the branch (with exponential backoff).
    waitForCreationOfLocalBranch(gitRepository, newBranchName);
  }

  @Override
  @UIEffect
  public void onThrowable(Throwable e) {
    val exceptionMessage = e.getMessage();
    VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
        /* title */ getString(
            "action.GitMachete.BaseSlideInBelowAction.notification.title.branch-layout-write-fail"),
        exceptionMessage.requireNonNullElse(""));
  }

  @Override
  public void onFinished() {
    graphTable.enableEnqueuingUpdates();
  }
}
