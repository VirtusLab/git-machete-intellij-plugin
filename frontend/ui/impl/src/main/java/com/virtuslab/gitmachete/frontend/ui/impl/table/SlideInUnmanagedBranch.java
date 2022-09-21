package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.function.Consumer;

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

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
@AllArgsConstructor
@CustomLog
public class SlideInUnmanagedBranch {

  private final Project project;
  private final String branchName;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;
  private final Consumer<Path> onFailurePathConsumer;
  private final Consumer<String> onSuccessInferredParentBranchNameConsumer;

  public void enqueue(Path macheteFilePath) {
    LOG.info("Enqueuing automatic discover");
    val selectedRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (selectedRepository == null) {
      LOG.error("Can't do automatic discover because of undefined selected repository");
      return;
    }
    Path rootDirPath = selectedRepository.getRootDirectoryPath().toAbsolutePath();
    Path mainGitDirPath = selectedRepository.getMainGitDirectoryPath().toAbsolutePath();
    Path worktreeGitDirPath = selectedRepository.getWorktreeGitDirectoryPath().toAbsolutePath();

    new Task.Backgroundable(project, getString("string.GitMachete.EnhancedGraphTable.automatic-discover.task-title")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        LOG.debug("Running infer parent for unmanaged branch slide in task");
        val inferParentRunResult = Try.of(() -> {
          val gitMacheteRepository = RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
              .getInstance(rootDirPath, mainGitDirPath, worktreeGitDirPath);
          val managedBranches = gitMacheteRepository.discoverLayoutAndCreateSnapshot().getManagedBranches();
          val eligibleLocalBranchNames = managedBranches.map(IManagedBranchSnapshot::getName).toSet();
          return gitMacheteRepository.inferParentForLocalBranch(eligibleLocalBranchNames, branchName);
        });

        if (inferParentRunResult.isFailure()) {
          LOG.debug("Inferring parent failed");
          val exception = inferParentRunResult.getCause();
          ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  exception.getMessage() != null ? exception.getMessage() : ""));
          return;
        }

        val inferredParent = inferParentRunResult.get();

        if (inferredParent == null) {
          LOG.debug("Inferred parent is null - executing on-failure consumer");
          onFailurePathConsumer.accept(macheteFilePath);
          return;
        }

        LOG.debug("Executing on-success consumer");
        onSuccessInferredParentBranchNameConsumer.accept(inferredParent.getName());
      }
    }.queue();
  }
}
