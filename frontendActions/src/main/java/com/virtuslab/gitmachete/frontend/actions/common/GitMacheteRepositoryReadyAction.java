package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 * </ul>
 */
public abstract class GitMacheteRepositoryReadyAction extends DumbAwareAction {

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    boolean isEnabled = ActionUtils.getGitMacheteRepository(anActionEvent).isDefined();
    anActionEvent.getPresentation().setEnabled(isEnabled);

    if (!isEnabled) {
      anActionEvent.getPresentation().setDescription("Action disabled due to undefined Git Machete repository");
    }
  }
}
