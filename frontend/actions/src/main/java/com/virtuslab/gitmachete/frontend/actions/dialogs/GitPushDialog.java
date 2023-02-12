package com.virtuslab.gitmachete.frontend.actions.dialogs;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.intellij.dvcs.push.PushSource;
import com.intellij.dvcs.push.PushSupport;
import com.intellij.dvcs.push.VcsPushOptionValue;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.qual.async.BackgroundableQueuedElsewhere;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public final class GitPushDialog extends VcsPushDialog {
  private final boolean isForcePushRequired;
  private final @UI Runnable doInUIThreadWhenReady;
  private final Action pushAction;

  @UIEffect
  public GitPushDialog(
      Project project,
      Repository repository,
      PushSource pushSource,
      boolean isForcePushRequired) {
    this(project, repository, pushSource, isForcePushRequired, () -> {});
  }

  @UIEffect
  public GitPushDialog(
      Project project,
      Repository repository,
      PushSource pushSource,
      boolean isForcePushRequired,
      @UI Runnable doInUIThreadWhenReady) {
    // Presented dialog shows commits for branches belonging to allRepositories, selectedRepositories and currentRepo.
    // The second and the third one have a higher priority of loading its commits.
    // From our perspective, we always have a single (pre-selected) repository, so we do not care about the priority.
    super(project, /* allRepositories */ java.util.List.of(repository),
        /* selectedRepositories */ java.util.List.of(repository), /* currentRepo */ null, pushSource);
    this.isForcePushRequired = isForcePushRequired;
    this.doInUIThreadWhenReady = doInUIThreadWhenReady;
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

  /**
   * Overridden to provide a way to perform an action once the push is completed successfully.
   * Compare to the {@link com.intellij.dvcs.push.ui.VcsPushDialog#push(boolean)}
   * which doesn't implement {@code onSuccess} callback.
   */
  @Override
  @BackgroundableQueuedElsewhere // passed on to `executeAfterRunningPrePushHandlers`
  @UIEffect
  public void push(boolean forcePush) {

    String title = getString("string.GitMachete.GitPushDialog.task-title");
    executeAfterRunningPrePushHandlers(new Task.Backgroundable(myProject, title) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        myController.push(forcePush);
      }

      @Override
      @UIEffect
      public void onSuccess() {
        doInUIThreadWhenReady.run();
      }
    });
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
