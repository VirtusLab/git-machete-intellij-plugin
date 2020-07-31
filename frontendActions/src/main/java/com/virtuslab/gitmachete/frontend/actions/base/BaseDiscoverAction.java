package com.virtuslab.gitmachete.frontend.actions.base;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import io.vavr.control.Try;
import lombok.CustomLog;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class BaseDiscoverAction extends DumbAwareAction implements IExpectsKeyProject {
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    Try.of(() -> {
      var selectedRepoProvider = getProject(anActionEvent).getService(SelectedGitRepositoryProvider.class)
          .getGitRepositorySelectionProvider();
      var selectedRepositoryOption = selectedRepoProvider.getSelectedGitRepository();
      assert selectedRepositoryOption.isDefined() : "Selected repository is undefined";
      var selectedRepository = selectedRepositoryOption.get();
      var mainDirPath = GitVfsUtils.getMainDirectoryPath(selectedRepository).toAbsolutePath();
      var gitDirPath = GitVfsUtils.getGitDirectoryPath(selectedRepository).toAbsolutePath();
      return RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
          .getInstance(mainDirPath, gitDirPath).discoverLayoutAndCreateSnapshot();
    })
        .onFailure(e -> GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(getProject(anActionEvent))
            .notifyError("Repository discover error", e.getMessage() != null ? e.getMessage() : ""), NON_MODAL))
        .onSuccess(repoSnapshot -> GuiUtils
            .invokeLaterIfNeeded(() -> GraphTableDialog.of(repoSnapshot, "Discovered branch tree").show(), NON_MODAL));
  }

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }
}
