package com.virtuslab.gitmachete.frontend.file.quickfix;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import java.util.Collections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@RequiredArgsConstructor
@ExtensionMethod({GitMacheteBundle.class})
public class CreateBranchQuickFix implements IntentionAction {

  private final String branch;
  private final String parentBranch;
  private final PsiFile macheteFile;
  private final @Nullable GitRepository gitRepository;

  @Override
  public @IntentionName String getText() {
    return getNonHtmlString("action.GitMachete.MacheteAnnotator.IntentionAction.create-nonexistent-branch")
        .format(branch, parentBranch);
  }

  @Override
  public @IntentionFamilyName String getFamilyName() {
    return "Git Machete";
  }

  @Override
  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(Project project, Editor editor, PsiFile file) {
    createNewBranchFromParent(project);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private void createNewBranchFromParent(Project project) {
    if (gitRepository != null) {
      GitBrancher.getInstance(project).createBranch(branch, Collections.singletonMap(gitRepository, parentBranch));
    } else {
      throw new RuntimeException("Unable to create new branch due to: git repository not found for " +
          macheteFile.getVirtualFile().getPath() + " file.");
    }
  }
}
