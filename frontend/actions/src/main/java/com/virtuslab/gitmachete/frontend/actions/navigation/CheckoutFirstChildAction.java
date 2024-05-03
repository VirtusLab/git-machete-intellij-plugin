package com.virtuslab.gitmachete.frontend.actions.navigation;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.base.BaseCheckoutAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@CustomLog
@ExtensionMethod(GitMacheteBundle.class)
public class CheckoutFirstChildAction extends BaseCheckoutAction
    implements
      IExpectsKeySelectedBranchName {

  @Override
  protected @Untainted String getNonExistentBranchMessage(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    return currentBranchName != null
        ? getNonHtmlString("action.GitMachete.CheckoutFirstChildAction.undefined.branch-name").fmt(currentBranchName)
        : getNonHtmlString("action.GitMachete.BaseCheckoutAction.undefined.current-branch");
  }

  @Override
  protected @Nullable String getTargetBranchName(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    val currentBranch = getManagedBranchByName(anActionEvent, currentBranchName);
    if (currentBranch != null) {
      val childBranches = currentBranch.getChildren();
      if (childBranches.nonEmpty()) {
        val targetBranch = childBranches.get(0);
        return targetBranch.getName();
      }
    }
    return null;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }
}
