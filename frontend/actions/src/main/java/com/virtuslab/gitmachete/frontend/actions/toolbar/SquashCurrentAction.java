package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSquashAction;

public class SquashCurrentAction extends BaseSquashAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    val branchNameOption = getNameOfBranchUnderAction(anActionEvent);
    val nonRootBranchOption = branchNameOption.flatMap(bn -> getManagedBranchByName(anActionEvent, bn))
        .flatMap(b -> b.isNonRoot() ? Option.of(b.asNonRoot()) : Option.none());
    val syncToParentStatus = nonRootBranchOption.map(b -> b.getSyncToParentStatus()).getOrNull();
    val numberOfCommits = nonRootBranchOption.map(b -> b.getCommits().length()).getOrNull();

    val branchName = branchNameOption.getOrNull();
    if (branchName != null && nonRootBranchOption.isEmpty()) {
      presentation.setVisible(false);

    } else if (branchName != null && numberOfCommits != null) {

      if (numberOfCommits < 2 || syncToParentStatus == InSyncButForkPointOff) {
        presentation.setVisible(false);
      } else {
        presentation.setText(getString("action.GitMachete.BaseSquashAction.text"));
        presentation.setDescription(getNonHtmlString("action.GitMachete.SquashCurrentAction.description"));
      }
    }
  }
}
