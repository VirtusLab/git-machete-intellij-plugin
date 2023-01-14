package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class OverrideForkPointBackgroundable extends SideEffectingBackgroundable {

  private final GitRepository gitRepository;
  private final INonRootManagedBranchSnapshot nonRootBranch;
  @Nullable
  private final ICommitOfManagedBranch selectedCommit;
  private final BaseEnhancedGraphTable graphTable;

  public OverrideForkPointBackgroundable(String title, GitRepository gitRepository,
      INonRootManagedBranchSnapshot nonRootBranch, BaseEnhancedGraphTable graphTable,
      @Nullable ICommitOfManagedBranch selectedCommit) {
    super(gitRepository.getProject(), title, "fork point override");
    this.gitRepository = gitRepository;
    this.nonRootBranch = nonRootBranch;
    this.graphTable = graphTable;
    this.selectedCommit = selectedCommit;
  }

  @Override
  @UIThreadUnsafe
  public void doRun(ProgressIndicator indicator) {
    if (selectedCommit != null) {
      LOG.debug("Enqueueing fork point override");
      overrideForkPoint(nonRootBranch, selectedCommit);
    } else {
      LOG.debug(
          "Commit selected to be the new fork point is null: most likely the action has been canceled from override-fork-point dialog");
    }
  }

  @UIThreadUnsafe
  private void overrideForkPoint(IManagedBranchSnapshot branch, ICommitOfManagedBranch forkPoint) {
    if (gitRepository != null && myProject != null) {
      val root = gitRepository.getRoot();
      setOverrideForkPointConfigValues(myProject, root, branch.getName(), forkPoint, branch.getPointedCommit());
    }

    // required since the change of .git/config is not considered as a change to VCS (and detected by the listener)
    graphTable.queueRepositoryUpdateAndModelRefresh();
  }

  @UIThreadUnsafe
  private void setOverrideForkPointConfigValues(
      Project project,
      VirtualFile root,
      String branchName,
      ICommitOfManagedBranch forkPoint,
      ICommitOfManagedBranch ancestorCommit) {
    val section = "machete";
    val subsectionPrefix = "overrideForkPoint";
    val to = "to";
    val whileDescendantOf = "whileDescendantOf";

    // Section spans the characters before the first dot
    // Name spans the characters after the last dot
    // Subsection is everything else
    val toKey = "${section}.${subsectionPrefix}.${branchName}.${to}";
    val whileDescendantOfKey = "${section}.${subsectionPrefix}.${branchName}.${whileDescendantOf}";

    try {
      GitConfigUtil.setValue(project, root, toKey, forkPoint.getHash());
    } catch (VcsException e) {
      LOG.info("Attempt to set '${toKey}' git config value failed: " + e.getMessage());
    }

    try {
      GitConfigUtil.setValue(project, root, whileDescendantOfKey, forkPoint.getHash());
    } catch (VcsException e) {
      LOG.info("Attempt to get '${whileDescendantOf}' git config value failed: " + e.getMessage());
    }
  }
}
