package com.virtuslab.gitmachete.frontend.actions;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.keys.DataKeys;

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
      return;
    }

    presentation.setEnabled(true);
    presentation.setVisible(true);
  }

  /**
   * This method relies on key `KEY_GIT_MACHETE_REPOSITORY` corresponding to a non-null value
   * and hence must always be called after checking the git machete repository readiness.
   * See {@link GitMacheteRepositoryReadyAction#update} and {@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}.
   */
  protected IGitMacheteRepository getMacheteRepository(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_REPOSITORY);
    assert gitMacheteRepository != null : "Can't get gitMacheteRepository";

    return gitMacheteRepository;
  }
}
