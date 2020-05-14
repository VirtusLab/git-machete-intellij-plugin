package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getCurrentBranchNameIfManaged;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.actions.common.BasePushBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class PushCurrentBranchAction extends BasePushBranchAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }
}
