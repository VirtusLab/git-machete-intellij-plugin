package com.virtuslab.gitmachete.frontend.actions.dialogs;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;

import com.intellij.dvcs.push.PushSource;
import com.intellij.dvcs.push.PushSupport;
import com.intellij.dvcs.push.VcsPushOptionValue;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.backgroundables.SideEffectingBackgroundable;
import com.virtuslab.qual.async.BackgroundableQueuedElsewhere;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public final class GitPushDialog extends VcsPushDialog {
  private final boolean isForcePushRequired;
  private final @Nullable JComponent traverseInfoComponent;
  private final @UI Runnable doInUIThreadWhenReady;

  @UIEffect
  public GitPushDialog(
      Project project,
      Repository repository,
      PushSource pushSource,
      boolean isForcePushRequired) {
    this(project, repository, pushSource, isForcePushRequired,
        /* traverseInfoComponent */ null, /* doInUIThreadWhenReady */ () -> {}, /* titlePrefix */ "");
  }

  @UIEffect
  public GitPushDialog(
      Project project,
      Repository repository,
      PushSource pushSource,
      boolean isForcePushRequired,
      @Nullable JComponent traverseInfoComponent,
      @UI Runnable doInUIThreadWhenReady,
      String titlePrefix) {
    // Presented dialog shows commits for branches belonging to allRepositories, selectedRepositories and currentRepo.
    // The second and the third one have a higher priority of loading its commits.
    // From our perspective, we always have a single (pre-selected) repository, so we do not care about the priority.
    super(project, /* allRepositories */ java.util.List.of(repository),
        /* selectedRepositories */ java.util.List.of(repository), /* currentRepo */ null, pushSource);
    this.isForcePushRequired = isForcePushRequired;
    this.traverseInfoComponent = traverseInfoComponent;
    this.doInUIThreadWhenReady = doInUIThreadWhenReady;

    // Note: since the class is final, `this` is already @Initialized at this point.

    init();
    setTitle(titlePrefix + " " + this.getTitle());
  }

  @Override
  public @Nullable JComponent createNorthPanel() {
    return traverseInfoComponent;
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
    Action cancelAction = getCancelAction();
    Action pushAction = new PushSwingAction();
    if (traverseInfoComponent != null) {
      cancelAction.putValue(Action.NAME, getNonHtmlString("action.GitMachete.BaseTraverseAction.dialog.quit"));
      return new Action[]{cancelAction, new SkipSwingAction(), pushAction, new PushAndQuitSwingAction(), getHelpAction()};
    } else {
      return new Action[]{pushAction, cancelAction, getHelpAction()};
    }
  }

  @Override
  protected Action getOKAction() {
    return new PushSwingAction();
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

    String title = getNonHtmlString("string.GitMachete.GitPushDialog.task-title");
    executeAfterRunningPrePushHandlers(new SideEffectingBackgroundable(myProject, title, "push") {
      @Override
      @UIThreadUnsafe
      public void doRun(ProgressIndicator indicator) {
        myController.push(forcePush);
      }

      @Override
      @UIEffect
      public void onSuccess() {
        doInUIThreadWhenReady.run();
      }
    });
  }

  @BackgroundableQueuedElsewhere // passed on to `executeAfterRunningPrePushHandlers`
  @UIEffect
  public void pushAndQuit() {

    String title = getNonHtmlString("string.GitMachete.GitPushDialog.task-title");
    executeAfterRunningPrePushHandlers(new SideEffectingBackgroundable(myProject, title, "push") {
      @Override
      @UIThreadUnsafe
      public void doRun(ProgressIndicator indicator) {
        myController.push(isForcePushRequired);
      }
    });
  }

  @Override
  public @Nullable VcsPushOptionValue getAdditionalOptionValue(PushSupport support) {
    return null;
  }

  private String getPushActionName(boolean hasAndQuit) {
    if (hasAndQuit) {
      return (isForcePushRequired ? "Force Push" : "Push") + " _and Quit";
    } else {
      return isForcePushRequired ? "Force _Push" : "_Push";
    }
  }

  private class PushSwingAction extends AbstractAction {
    @UIEffect
    PushSwingAction() {
      super(getPushActionName(/* hasAndQuit */ false));
      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    @UIEffect
    public void actionPerformed(ActionEvent e) {
      push();
    }
  }

  private class PushAndQuitSwingAction extends AbstractAction {

    @UIEffect
    PushAndQuitSwingAction() {
      super(getPushActionName(/* hasAndQuit */ true));
    }

    @Override
    @UIEffect
    public void actionPerformed(ActionEvent e) {
      pushAndQuit();
    }
  }

  private class SkipSwingAction extends AbstractAction {

    @UIEffect
    SkipSwingAction() {
      super("_Skip Push");
    }

    @Override
    @UIEffect
    public void actionPerformed(ActionEvent e) {
      close(OK_EXIT_CODE);
      doInUIThreadWhenReady.run();
    }
  }
}
