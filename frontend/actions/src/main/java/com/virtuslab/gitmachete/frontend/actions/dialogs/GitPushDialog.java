package com.virtuslab.gitmachete.frontend.actions.dialogs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.intellij.dvcs.push.PushSource;
import com.intellij.dvcs.push.PushSupport;
import com.intellij.dvcs.push.VcsPushOptionValue;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.project.Project;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GitPushDialog extends VcsPushDialog {
  private final boolean isForcePushRequired;
  private final Action pushAction;

  @UIEffect
  public GitPushDialog(
      Project project,
      Repository repository,
      PushSource pushSource,
      boolean isForcePushRequired) {
    // Presented dialog shows commits for branches belonging to allRepositories, selectedRepositories and currentRepo.
    // The second and the third one have a higher priority of loading its commits.
    // From our perspective, we always have a single (pre-selected) repository, so we do not care about the priority.
    super(project, /* allRepositories */ java.util.List.of(repository),
        /* selectedRepositories */ java.util.List.of(repository), /* currentRepo */ null, pushSource);
    this.isForcePushRequired = isForcePushRequired;
    this.pushAction = new PushSwingAction();

    // Note: since the class is final, `this` is already @Initialized at this point.

    setOKButtonText(getPushActionName());
    init();
  }

  @Override
  @UIEffect
  public void updateOkActions() {}

  @Override
  @UIEffect
  protected void doOKAction() {
    push();
  }

  @Override
  @UIEffect
  protected Action[] createActions() {
    return new Action[]{pushAction, getCancelAction(), getHelpAction()};
  }

  @Override
  protected Action getOKAction() {
    return pushAction;
  }

  @UIEffect
  private void push() {
    push(isForcePushRequired);
  }

  @Override
  public @Nullable VcsPushOptionValue getAdditionalOptionValue(PushSupport support) {
    return null;
  }

  private String getPushActionName() {
    return isForcePushRequired ? "Force _Push" : "_Push";
  }

  private class PushSwingAction extends AbstractAction {

    @UIEffect
    PushSwingAction() {
      super(getPushActionName());
      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    @UIEffect
    public void actionPerformed(ActionEvent e) {
      push();
    }
  }
}
