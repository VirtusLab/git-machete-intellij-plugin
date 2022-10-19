package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
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
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public class SlideInNonRootBackgroundable extends BaseSlideInBackgroundable {

  private final String parentName;

  public SlideInNonRootBackgroundable(
      Project project,
      GitRepository gitRepository,
      BranchLayout branchLayout,
      IBranchLayoutWriter branchLayoutWriter,
      Runnable preSlideInRunnable,
      SlideInOptions slideInOptions,
      String parentName) {
    super(project, gitRepository, branchLayout, branchLayoutWriter, preSlideInRunnable, slideInOptions);
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
              .format(parentName),
          e);
      return null;
    } catch (EntryIsDescendantOfException e) {
      notifyError(
          getString("action.GitMachete.SlideInNonRootBackgroundable.notification.message.entry-is-descendant-of.HTML")
              .format(entryToSlideIn.getName(), parentName),
          e);
      return null;
    }
  }

  private void notifyError(@Nullable String message, Throwable throwable) {
    VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
        /* title */ getString("action.GitMachete.SlideInNonRootBackgroundable.notification.title.slide-in-fail.HTML")
            .format(slideInOptions.getName()),
        message != null ? message : getMessageOrEmpty(throwable));
  }

}
