package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSquashAction;

@CustomLog
public class SquashCurrentAction extends BaseSquashAction {
  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    super.onUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val managedBranch = getManagedBranchByName(anActionEvent, branchName);
    val nonRootBranch = managedBranch != null && managedBranch.isNonRoot()
        ? managedBranch.asNonRoot()
        : null;
    val syncToParentStatus = nonRootBranch != null ? nonRootBranch.getSyncToParentStatus() : null;

    if (branchName != null) {
      if (nonRootBranch == null) {
        presentation.setVisible(false);

      } else {
        val numberOfCommits = nonRootBranch.getUniqueCommits().length();

        if (numberOfCommits < 2 || syncToParentStatus == InSyncButForkPointOff) {
          presentation.setVisible(false);
        } else {
          presentation.setText(getString("action.GitMachete.BaseSquashAction.text"));
          presentation.setDescription(getNonHtmlString("action.GitMachete.SquashCurrentAction.description"));
        }
      }
    }
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }
}
