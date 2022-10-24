package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.util.ModalityUiUtil;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;

import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
@AllArgsConstructor
@CustomLog
public class SlideInUnmanagedBranch {

  private final Project project;
  private final Supplier<Try<ILocalBranchReference>> inferParentResultSupplier;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;
  private final Consumer<ILocalBranchReference> onSuccessInferredParentBranchConsumer;

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
        val inferParentResult = inferParentResultSupplier.get();
        if (inferParentResult.isFailure()) {
          LOG.debug("Inferring parent failed");
          val exception = inferParentResult.getCause();
          ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  exception.getMessage() != null ? exception.getMessage() : ""));
          return;
        }

        val inferredParent = inferParentResult.get();

        LOG.debug("Executing on-success consumer");
        onSuccessInferredParentBranchConsumer.accept(inferredParent);
      }
    }.queue();
  }
}
