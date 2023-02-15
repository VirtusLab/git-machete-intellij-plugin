package com.virtuslab.gitmachete.frontend.file;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
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
  public static List<String> getBranchNamesForPsiMacheteFile(PsiFile psiFile) {
    val gitRepository = findGitRepositoryForPsiMacheteFile(psiFile);

    if (gitRepository == null) {
      return List.empty();
    }

    return List.ofAll(gitRepository.getInfo().getLocalBranchesWithHashes().keySet())
        .map(localBranch -> localBranch.getName());
  }

  @UIThreadUnsafe
  public static @Nullable GitRepository findGitRepositoryForPsiMacheteFile(PsiFile psiFile) {
    val project = psiFile.getProject();
    return List.ofAll(GitRepositoryManager.getInstance(project).getRepositories())
        .find(repository -> {
          val macheteFile = repository.getMacheteFile();
          return macheteFile != null && macheteFile.equals(psiFile.getVirtualFile());
        }).getOrNull();
  }

  @UIEffect
  public static void saveDocument(PsiFile file) {
    val fileDocManager = FileDocumentManager.getInstance();
    val document = fileDocManager.getDocument(file.getVirtualFile());
    if (document != null) {
      fileDocManager.saveDocument(document);
    }
  }

  public static @Nullable VirtualFile getMacheteVirtualFileIfSelected(Project project) {
    val fileEditorManager = FileEditorManager.getInstance(project);
    return List.of(fileEditorManager.getSelectedFiles())
        .find(virtualFile -> virtualFile.getFileType().equals(MacheteFileType.instance)).getOrNull();
  }

  /**
   * "Selected" = open AND focused.
   * Note that there can be multiple selected files in the given project, e.g. in case of split editors.
   */
  public static boolean isMacheteFileSelected(Project project) {
    return getMacheteVirtualFileIfSelected(project) != null;
  }
}
