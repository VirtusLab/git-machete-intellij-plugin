package com.virtuslab.gitmachete.frontend.actions.dialogs;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.tainting.qual.Untainted;

@RequiredArgsConstructor
public class TraverseStepConfirmationDialog {

  private final String title;

  private final String message;

  public enum Result {
    YES, YES_AND_QUIT, NO, QUIT
  }

  @UIEffect
  public Result show(Project project) {
    val yesText = getNonHtmlString("action.GitMachete.BaseTraverseAction.dialog.yes");
    val yesAndQuitText = getNonHtmlString("action.GitMachete.BaseTraverseAction.dialog.yes-and-quit");
    val noText = getNonHtmlString("action.GitMachete.BaseTraverseAction.dialog.no");
    val quitText = getNonHtmlString("action.GitMachete.BaseTraverseAction.dialog.quit");
    @Untainted String[] options = {yesText, yesAndQuitText, noText, quitText};
    int result = Messages.showDialog(project, message, title, options, /* defaultOptionIndex */ 0, Messages.getQuestionIcon());

    switch (result) {
      case 0 :
        return Result.YES;
      case 1 :
        return Result.YES_AND_QUIT;
      case 2 :
        return Result.NO;
      default :
        return Result.QUIT;
    }
  }
}
