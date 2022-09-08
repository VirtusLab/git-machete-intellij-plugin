package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSyncToParentByMergeAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.WarnAboutRebaseToParentByMergeDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

public class SyncSelectedToParentByMergeAction extends BaseSyncToParentByMergeAction
    implements
      IExpectsKeySelectedBranchName {

  public static final String MERGE_INFO_SHOWN = "git-machete.reset.info.shown";

  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    if (!PropertiesComponent.getInstance().getBoolean(MERGE_INFO_SHOWN)) {

      val dialogBuilder = MessageDialogBuilder.okCancel(
          getString("action.GitMachete.SyncSelectedToParentByMergeAction.warning-dialog.title"),
          getString("action.GitMachete.SyncSelectedToParentByMergeAction.warning-dialog.message"));

      dialogBuilder.yesText(getString("action.GitMachete.SyncSelectedToParentByMergeAction.warning-dialog.ok-text"))
          .icon(Messages.getWarningIcon())
          .doNotAsk(new WarnAboutRebaseToParentByMergeDialog());

      val dialogResult = dialogBuilder.ask(getProject(anActionEvent));

      if (!dialogResult) {
        return;
      }
    }

    super.actionPerformed(anActionEvent);
  }
}
