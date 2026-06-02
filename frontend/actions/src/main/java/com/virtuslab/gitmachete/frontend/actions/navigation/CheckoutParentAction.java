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
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@CustomLog
@ExtensionMethod(GitMacheteBundle.class)
public class CheckoutParentAction extends BaseCheckoutAction {

  @Override
  protected @Untainted String getNonExistentBranchMessage(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    return currentBranchName != null
        ? getNonHtmlString("action.GitMachete.CheckoutParentAction.undefined.branch-name").fmt(currentBranchName)
        : getNonHtmlString("action.GitMachete.BaseCheckoutAction.undefined.current-branch");
  }

  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    val currentBranch = getManagedBranchByName(anActionEvent, currentBranchName);
    if (currentBranch != null && currentBranch.isNonRoot()) {
      val nonRootBranch = currentBranch.asNonRoot();
      return nonRootBranch.getParent().getName();
    }
    return null;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }
}
