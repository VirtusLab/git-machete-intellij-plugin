package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public class SlideInRootBackgroundable extends BaseSlideInBackgroundable {

  public SlideInRootBackgroundable(
      GitRepository gitRepository,
      BranchLayout branchLayout,
      IBranchLayoutWriter branchLayoutWriter,
      BaseEnhancedGraphTable graphTable,
      Runnable preSlideInRunnable,
      SlideInOptions slideInOptions) {
    super(gitRepository, branchLayout, branchLayoutWriter, graphTable, preSlideInRunnable, slideInOptions);
  }

  @Override
  public @Nullable BranchLayout deriveNewBranchLayout(BranchLayout targetBranchLayout, BranchLayoutEntry entryToSlideIn) {
    return new BranchLayout(targetBranchLayout.getRootEntries().append(entryToSlideIn));
  }

}
