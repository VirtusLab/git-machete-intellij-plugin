package com.virtuslab.gitmachete.frontend.file;

import com.intellij.psi.PsiFile;
import git4idea.repo.GitRepositoryManager;
import io.vavr.collection.List;
import lombok.val;

import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public final class MacheteFileUtils {
  private MacheteFileUtils() {}

  @UIThreadUnsafe
  public static List<String> getBranchNamesForPsiFile(PsiFile psiFile) {
    val project = psiFile.getProject();

    val gitRepository = List.ofAll(GitRepositoryManager.getInstance(project).getRepositories())
        .find(repository -> GitVfsUtils.getMacheteFile(repository)
            .map(macheteFile -> macheteFile.equals(psiFile.getVirtualFile())).getOrElse(false));

    if (gitRepository.isEmpty()) {
      return List.empty();
    }

    return List.ofAll(gitRepository.get().getInfo().getLocalBranchesWithHashes().keySet())
        .map(localBranch -> localBranch.getName());
  }
}
