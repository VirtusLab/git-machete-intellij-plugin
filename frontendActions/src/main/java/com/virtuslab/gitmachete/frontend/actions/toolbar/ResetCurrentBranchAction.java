package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGitMacheteRepository;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.common.BaseResetBranchAction;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class ResetCurrentBranchAction extends BaseResetBranchAction {
  @Override
  protected Option<String> getBranchName(AnActionEvent anActionEvent) {
    return getGitMacheteRepository(anActionEvent)
        .flatMap(IGitMacheteRepository::getCurrentBranchIfManaged)
        .map(BaseGitMacheteBranch::getName);
  }

  @Override
  protected String getActionType() {
    return "current";
  }
}
