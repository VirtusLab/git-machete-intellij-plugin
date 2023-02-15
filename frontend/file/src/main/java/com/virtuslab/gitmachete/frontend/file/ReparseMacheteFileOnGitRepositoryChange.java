package com.virtuslab.gitmachete.frontend.file;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.file.MacheteFileUtils.getMacheteVirtualFileIfSelected;

import com.intellij.openapi.project.Project;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.ModalityUiUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ReparseMacheteFileOnGitRepositoryChange implements GitRepositoryChangeListener {

  private final Project project;

  @Override
  public void repositoryChanged(GitRepository repository) {
    // Note that if machete file is just opened but NOT selected,
    // then it's apparently always getting reparsed once selected.
    // The only problematic case is when machete file is already selected, and the underlying git repository changes.
    // Unless a reparsing is forced, red squiggles marking a non-existent branch will stick around
    // even once that branch has already been created, e.g. by user firing our Alt+Enter quick fix.
    val macheteVirtualFile = getMacheteVirtualFileIfSelected(project);
    if (macheteVirtualFile != null) {
      ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> FileContentUtilCore.reparseFiles(macheteVirtualFile));
    }
  }
}
