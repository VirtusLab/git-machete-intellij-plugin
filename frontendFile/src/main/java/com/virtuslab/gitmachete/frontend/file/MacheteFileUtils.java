package com.virtuslab.gitmachete.frontend.file;

import com.intellij.psi.PsiFile;
import git4idea.repo.GitRepositoryManager;
import io.vavr.collection.List;
import io.vavr.control.Option;

public final class MacheteFileUtils {
  private MacheteFileUtils() {}

  public static Option<List<String>> getBranchNamesForFile(PsiFile psiFile) {
    var project = psiFile.getProject();

    var gitRepository = List.ofAll(GitRepositoryManager.getInstance(project).getRepositories())
        .find(r -> psiFile.getVirtualFile().getPath().startsWith(r.getRoot().getPath()));

    if (gitRepository.isEmpty()) {
      return Option.none();
    }

    var branchNames = List.ofAll(gitRepository.get().getInfo().getLocalBranchesWithHashes().keySet())
        .map(localBranch -> localBranch.getName());

    return Option.of(branchNames);
  }
}
