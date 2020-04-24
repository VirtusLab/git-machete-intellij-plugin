package com.virtuslab.gitmachete.frontend.actions;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}</li>
 * </ul>
 */
public abstract class GitMacheteRepositoryReadyAction extends DumbAwareAction {

  public GitMacheteRepositoryReadyAction(String text, String description, Icon icon) {
    super(text, description, icon);
  }

  public GitMacheteRepositoryReadyAction() {}

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    var isReady = anActionEvent.getData(DataKeys.KEY_IS_GIT_MACHETE_REPOSITORY_READY);
    if (isReady == null || !isReady) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
    } else {
      presentation.setEnabled(true);
      presentation.setVisible(true);
    }
  }
}
