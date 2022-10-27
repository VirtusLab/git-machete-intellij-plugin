package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.OverrideForkPointDialog;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class OverrideForkPointBackgroundable extends Task.Backgroundable {

  private final GitRepository gitRepository;

  private final INonRootManagedBranchSnapshot nonRootBranch;

  private final ICommitOfManagedBranch selectedCommit;

  private final boolean shouldOverrideForkPoint;

  private final BaseEnhancedGraphTable graphTable;

  public LambdaLogger log() {
    return LOG;
  }

  public OverrideForkPointBackgroundable(Project project, String title, GitRepository gitRepository,
      INonRootManagedBranchSnapshot nonRootBranch, BaseEnhancedGraphTable graphTable) {
    super(project, title);
    this.gitRepository = gitRepository;
    this.nonRootBranch = nonRootBranch;
    this.graphTable = graphTable;
    selectedCommit = new OverrideForkPointDialog(project, nonRootBranch).showAndGetSelectedCommit();
    if (selectedCommit == null) {
      log().debug(
          "Commit selected to be the new fork point is null: most likely the action has been canceled from override-fork-point dialog");
      shouldOverrideForkPoint = false;
    } else {
      shouldOverrideForkPoint = true;
    }

    LOG.debug("Enqueueing fork point override");

  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    if (shouldOverrideForkPoint) {
      overrideForkPoint(nonRootBranch, selectedCommit);
    }
  }

  @UIThreadUnsafe
  private void overrideForkPoint(IManagedBranchSnapshot branch, ICommitOfManagedBranch forkPoint) {

    if (gitRepository != null && myProject != null) {
      val root = gitRepository.getRoot();
      setOverrideForkPointConfigValues(myProject, root, branch.getName(), forkPoint, branch.getPointedCommit());
    }

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
