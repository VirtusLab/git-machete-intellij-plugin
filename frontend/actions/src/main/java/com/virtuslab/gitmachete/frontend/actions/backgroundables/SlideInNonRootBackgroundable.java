package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Objects;

import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.EntryDoesNotExistException;
import com.virtuslab.branchlayout.api.EntryIsDescendantOfException;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.common.SlideInOptions;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class, Objects.class})
public class SlideInNonRootBackgroundable extends BaseSlideInBackgroundable {

  private final String parentName;

  public SlideInNonRootBackgroundable(
      GitRepository gitRepository,
      BranchLayout branchLayout,
      IBranchLayoutWriter branchLayoutWriter,
      BaseEnhancedGraphTable graphTable,
      Runnable preSlideInRunnable,
      SlideInOptions slideInOptions,
      String parentName) {
    super(gitRepository, branchLayout, branchLayoutWriter, graphTable, preSlideInRunnable, slideInOptions);
    this.parentName = parentName;
  }

  @Override
  @Nullable
  BranchLayout deriveNewBranchLayout(BranchLayout targetBranchLayout, BranchLayoutEntry entryToSlideIn) {
    try {
      return targetBranchLayout.slideIn(parentName, entryToSlideIn);
    } catch (EntryDoesNotExistException e) {
      notifyError(
          getString("action.GitMachete.SlideInNonRootBackgroundable.notification.message.entry-does-not-exist.HTML")
              .fmt(parentName),
          e);
      return null;
    } catch (EntryIsDescendantOfException e) {
      notifyError(
          getString("action.GitMachete.SlideInNonRootBackgroundable.notification.message.entry-is-descendant-of.HTML")
              .fmt(entryToSlideIn.getName(), parentName),
          e);
      return null;
    }
  }

  private void notifyError(@Nullable String message, Throwable throwable) {
    VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
        /* title */ getString("action.GitMachete.SlideInNonRootBackgroundable.notification.title.slide-in-fail.HTML")
            .fmt(slideInOptions.getName()),
        message.requireNonNullElse(throwable.getMessage().requireNonNullElse("")));
  }
}
