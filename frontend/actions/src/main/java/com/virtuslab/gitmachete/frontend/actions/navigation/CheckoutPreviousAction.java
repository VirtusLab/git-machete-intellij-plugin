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
public class CheckoutPreviousAction extends BaseCheckoutAction {

  @Override
  protected @Untainted String getNonExistentBranchMessage(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    return currentBranchName != null
        ? getNonHtmlString("action.GitMachete.CheckoutPreviousAction.undefined.branch-name").fmt(currentBranchName)
        : getNonHtmlString("action.GitMachete.BaseCheckoutAction.undefined.current-branch");
  }

  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);
    if (branchLayout != null && currentBranchName != null) {
      val previousEntry = branchLayout.findPreviousEntry(currentBranchName);
      return previousEntry != null ? previousEntry.getName() : null;
    }
    return null;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }
}
