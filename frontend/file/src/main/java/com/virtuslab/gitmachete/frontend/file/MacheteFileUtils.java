package com.virtuslab.gitmachete.frontend.file;

import com.intellij.psi.PsiFile;
import git4idea.repo.GitRepositoryManager;
import io.vavr.collection.List;
import lombok.val;

import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public final class MacheteFileUtils {
  private MacheteFileUtils() {}

  public static String getSampleMacheteFileContents() {
    // We're deliberately using \n rather than `System.lineSeparator()` here
    // since it turned out that even on Windows (which generally uses \r\n) IntelliJ expects \n in Color Settings code sample
    // (and probably in Code Style as well).
    @SuppressWarnings("regexp") String NL = "\n";

    return "develop" + NL +
        "\tallow-ownership-link PR #123" + NL +
        "\t\tbuild-chain" + NL +
        "\tcall-ws PR #124" + NL +
        NL +
        "master" + NL +
        "\thotfix/add-trigger PR #127";
  }

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
