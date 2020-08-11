package com.virtuslab.gitmachete.frontend.file;

import com.intellij.psi.PsiFile;
import git4idea.repo.GitRepositoryManager;
import io.vavr.collection.List;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

public final class MacheteFileUtils {
  private MacheteFileUtils() {}

  public static Option<List<String>> getBranchNamesForPsiFile(PsiFile psiFile) {
    var project = psiFile.getProject();

    var gitRepository = List.ofAll(GitRepositoryManager.getInstance(project).getRepositories())
        .find(repository -> GitVfsUtils.getMacheteFile(repository)
            .map(macheteFile -> macheteFile.equals(psiFile.getVirtualFile())).getOrElse(false));

    if (gitRepository.isEmpty()) {
      return Option.none();
    }

    var branchNames = List.ofAll(gitRepository.get().getInfo().getLocalBranchesWithHashes().keySet())
        .map(localBranch -> localBranch.getName());

    return Option.of(branchNames);
  }
}
