package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class SlideOutBackgroundable extends Task.Backgroundable {

  private final String branchToSlideOutName;
  @Nullable
  private final String currentBranchNameIfManaged;
  private final BranchLayout branchLayout;
  @Nullable
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;

  public SlideOutBackgroundable(Project project, String title, IManagedBranchSnapshot branchToSlideOut,
      @Nullable GitRepository gitRepository,
      @Nullable IManagedBranchSnapshot currentBranchNameIfManaged,
      BranchLayout branchLayout,
      BaseEnhancedGraphTable graphTable) {
    super(project, title);
    this.branchToSlideOutName = branchToSlideOut.getName();
    this.currentBranchNameIfManaged = currentBranchNameIfManaged != null ? currentBranchNameIfManaged.getName() : null;
    this.branchLayout = branchLayout;
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;

    LOG.debug(() -> "Entering: branchToSlideOut = ${branchToSlideOutName}");
    LOG.debug("Refreshing repository state");
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    if (gitRepository != null) {
      val slideOutBranchIsCurrent = branchToSlideOutName.equals(currentBranchNameIfManaged);
      new SlideOutAndDeleteHandler(gitRepository,
          graphTable,
          branchLayout,
          branchToSlideOutName,
          slideOutBranchIsCurrent).handle();
    } else {
      graphTable.queueRepositoryUpdateAndModelRefresh();
    }
  }

}
