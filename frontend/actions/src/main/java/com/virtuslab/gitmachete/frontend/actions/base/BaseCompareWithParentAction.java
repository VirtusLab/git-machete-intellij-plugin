package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.fmt;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import git4idea.branch.GitBrancher;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public abstract class BaseCompareWithParentAction extends BaseGitMacheteRepositoryReadyAction implements IBranchNameProvider {

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val managedBranch = getManagedBranchByName(anActionEvent, branchName);

    if (gitRepository == null || managedBranch == null) {
      return;
    }

    if (managedBranch.isRoot()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          fmt(getNonHtmlString("action.GitMachete.BaseCompareWithParentAction.description.disabled.branch-is-root"),
              managedBranch.getName()));
      return;
    }

    val parent = managedBranch.asNonRoot().getParent();

    String description = fmt(getNonHtmlString("action.GitMachete.BaseCompareWithParentAction.description.precise"),
        parent.getName(),
        managedBranch.getName());
    presentation.setDescription(description);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    FileDocumentManager.getInstance().saveAllDocuments();

    val project = getProject(anActionEvent);
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val managedBranch = getManagedBranchByName(anActionEvent, branchName);

    if (gitRepository == null || managedBranch == null || managedBranch.isRoot()) {
      return;
    }

    val parent = managedBranch.asNonRoot().getParent();
    val repositories = Collections.singletonList(gitRepository);
    GitBrancher.getInstance(project).compareAny(parent.getName(), managedBranch.getName(), repositories);
  }

}
