package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.util.ModalityUiUtil;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@AllArgsConstructor
@CustomLog
public abstract class SlideInUnmanagedBranch {

  private final Project project;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;

  @UIThreadUnsafe
  protected abstract @Nullable ILocalBranchReference inferParent() throws GitMacheteException;

  protected abstract void onInferParentSuccess(ILocalBranchReference inferredParent);

  @ContinuesInBackground
  public void enqueue() {
    LOG.info("Enqueuing unmanaged branch notification");
    val selectedRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (selectedRepository == null) {
      LOG.error("Can't notify about unmanaged branch because of undefined selected repository");
      return;
    }

    new Task.Backgroundable(project,
        getString("string.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.task-title")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        LOG.debug("Running infer parent for unmanaged branch slide in task");
        try {
          val inferredParent = inferParent();
          if (inferredParent != null) {
            onInferParentSuccess(inferredParent);
          }
        } catch (GitMacheteException e) {
          LOG.debug("Inferring parent failed");
          ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  e.getMessage() != null ? e.getMessage() : ""));
        }
      }
    }.queue();
  }
}
