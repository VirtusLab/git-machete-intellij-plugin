package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSyncToParentByMergeAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.WarnAboutSyncToParentByMergeDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;

public class SyncSelectedToParentByMergeAction extends BaseSyncToParentByMergeAction
    implements
      IExpectsKeySelectedBranchName {

  public static final String SHOW_MERGE_WARNING = "git-machete.merge.warning.show";

  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val project = getProject(anActionEvent);
    if (PropertiesComponent.getInstance(project).getBoolean(SHOW_MERGE_WARNING, /* defaultValue */ true)) {

      val dialogBuilder = MessageDialogBuilder.okCancel(
          getString("action.GitMachete.SyncSelectedToParentByMergeAction.warning-dialog.title"),
          getString("action.GitMachete.SyncSelectedToParentByMergeAction.warning-dialog.message.HTML"));

      dialogBuilder
          .icon(Messages.getWarningIcon())
          .doNotAsk(new WarnAboutSyncToParentByMergeDialog(project));

      val dialogResult = dialogBuilder.ask(project);

      if (!dialogResult) {
        return;
      }
    }

    super.actionPerformed(anActionEvent);
  }
}
