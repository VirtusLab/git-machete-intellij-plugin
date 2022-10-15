package com.virtuslab.gitmachete.frontend.file.quickfix;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import java.util.Collections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.experimental.ExtensionMethod;
import org.jetbrains.annotations.NotNull;

import com.virtuslab.gitmachete.frontend.file.MacheteFileUtils;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod({GitMacheteBundle.class})
public class CreateBranchQuickFix implements IntentionAction {

  private final String branch;
  private final String parentBranch;
  private final PsiFile macheteFile;

  public CreateBranchQuickFix(String processedBranchName, String parentBranchName, PsiFile processedFile) {
    branch = processedBranchName;
    parentBranch = parentBranchName;
    macheteFile = processedFile;
  }

  @Override
  public @IntentionName @NotNull String getText() {
    return getNonHtmlString("action.GitMachete.BaseSlideInBelowAction.dialog.create-new-branch.title")
        .format(parentBranch) + ": " + branch;
  }

  @Override
  public @NotNull @IntentionFamilyName String getFamilyName() {
    return "Git Machete";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    createNewBranchFromParent(project);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private void createNewBranchFromParent(Project project) {
    Option<GitRepository> gitRepositoryOption = MacheteFileUtils.findGitRepositoryForPsiMacheteFile(macheteFile);//.get();
    if (gitRepositoryOption.isDefined()) {
      GitBrancher.getInstance(project).createBranch(branch, Collections.singletonMap(gitRepositoryOption.get(), parentBranch));
    } else {
      throw new RuntimeException("Unable to create new branch due to: git repository not found for .git/machete file.");
    }
  }
}
