package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.branch.GitBrancher;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public abstract class BaseCheckoutAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeySelectedBranchName {
  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  protected abstract @Nullable String getTargetBranchName(AnActionEvent anActionEvent);

  protected abstract @Untainted String getNonExistentBranchMessage(AnActionEvent anActionEvent);

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val targetBranchName = getTargetBranchName(anActionEvent);

    // It's very unlikely that targetBranchName is empty at this point since it's assigned directly before invoking this
    // action in EnhancedGraphTable.EnhancedGraphTableMouseAdapter#mouseClicked; still, it's better to be safe.
    if (targetBranchName == null || targetBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonExistentBranchMessage(anActionEvent));
      return;
    }

    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);

    if (currentBranchName != null && currentBranchName.equals(targetBranchName)) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseCheckoutAction.description.disabled.currently-checked-out")
              .fmt(targetBranchName));

    } else {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseCheckoutAction.description.precise").fmt(targetBranchName));
    }
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val targetBranchName = getTargetBranchName(anActionEvent);
    if (targetBranchName == null || targetBranchName.isEmpty()) {
      return;
    }

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository != null) {
      log().debug(() -> "Queuing '${targetBranchName}' branch checkout background task");
      GitBrancher.getInstance(project).checkout(/* reference */ targetBranchName, /* detach */ false,
          Collections.singletonList(gitRepository), /* callInAwtLater */ () -> {});
    }
  }
}
