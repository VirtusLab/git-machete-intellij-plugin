package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

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
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class OverrideForkPointBackgroundable extends SideEffectingBackgroundable {

  private final GitRepository gitRepository;
  private final INonRootManagedBranchSnapshot nonRootBranch;
  @Nullable
  private final ICommitOfManagedBranch selectedCommit;
  private final BaseEnhancedGraphTable graphTable;

  public OverrideForkPointBackgroundable(GitRepository gitRepository, INonRootManagedBranchSnapshot nonRootBranch,
      BaseEnhancedGraphTable graphTable, @Nullable ICommitOfManagedBranch selectedCommit) {
    super(gitRepository.getProject(), getNonHtmlString("action.GitMachete.OverrideForkPointBackgroundable.task.title"),
        "fork point override");
    this.gitRepository = gitRepository;
    this.nonRootBranch = nonRootBranch;
    this.graphTable = graphTable;
    this.selectedCommit = selectedCommit;
  }

  @Override
  @ContinuesInBackground
  @UIThreadUnsafe
  public void doRun(ProgressIndicator indicator) {
    if (selectedCommit != null) {
      LOG.debug("Enqueueing fork point override");
      overrideForkPoint(nonRootBranch, selectedCommit);
    } else {
      LOG.debug("Commit selected to be the new fork point is null: " +
          "most likely the action has been canceled from override-fork-point dialog");
    }
  }

  @ContinuesInBackground
  @UIThreadUnsafe
  private void overrideForkPoint(IManagedBranchSnapshot branch, ICommitOfManagedBranch forkPoint) {
    if (gitRepository != null) {
      val root = gitRepository.getRoot();
      setOverrideForkPointConfigValues(project, root, branch.getName(), forkPoint);
    }

    // required since the change of .git/config is not considered as a change to VCS (and detected by the listener)
    graphTable.queueRepositoryUpdateAndModelRefresh();
  }

  @UIThreadUnsafe
  private void setOverrideForkPointConfigValues(
      Project project,
      VirtualFile root,
      String branchName,
      ICommitOfManagedBranch forkPoint) {
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
      // As a step towards deprecation of `whileDescendantOf` key (see #1580),
      // let's just set its value to the same as `to` key.
      // This will mitigate the problem pointed out in https://github.com/VirtusLab/git-machete/issues/611,
      // while not breaking older git-machete clients (esp. CLI) that still require `whileDescendantOf` key to be present
      // for a fork point override to be valid.
      GitConfigUtil.setValue(project, root, whileDescendantOfKey, forkPoint.getHash());
    } catch (VcsException e) {
      LOG.info("Attempt to get '${whileDescendantOf}' git config value failed: " + e.getMessage());
    }
  }
}
