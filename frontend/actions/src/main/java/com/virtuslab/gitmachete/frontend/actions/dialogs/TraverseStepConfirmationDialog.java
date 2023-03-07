package com.virtuslab.gitmachete.frontend.actions.dialogs;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.MessageDialogBuilder;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.guieffect.qual.UIEffect;

@RequiredArgsConstructor
public class TraverseStepConfirmationDialog {

  private final String title;

  private final String message;

  public enum Result {
    YES, YES_AND_QUIT, NO, QUIT
  }

  @UIEffect
  public Result show(Project project) {
    int result = MessageDialogBuilder.yesNoCancel(title, message)
        .cancelText(getNonHtmlString("action.GitMachete.BaseTraverseAction.dialog.cancel-traverse"))
        .show(project);

    switch (result) {
      case MessageConstants.YES :
        return Result.YES;
      case MessageConstants.NO :
        return Result.NO;
      default :
        return Result.QUIT;
    }
  }
}
