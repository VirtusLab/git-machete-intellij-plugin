package com.virtuslab.gitmachete.frontend.actions.dialogs;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

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
  private final @UI Runnable doInUIThreadWhenReady;
  private final Action pushAction;
  private final Project project;

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
    this.project = project;

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
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    push();
  }

  @Override
  @UIEffect
  protected Action[] createActions() {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    return new Action[]{pushAction, getCancelAction(), getHelpAction()};
  }

  @Override
  protected Action getOKAction() {
    return pushAction;
  }

  @UIEffect
  private void push() {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
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
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }

    String title = getNonHtmlString("string.GitMachete.GitPushDialog.task-title");
    executeAfterRunningPrePushHandlers(new SideEffectingBackgroundable(project, title, /* name */ "push") {
      @Override
      @UIThreadUnsafe
      public void doRun(ProgressIndicator indicator) {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
          var sw = new java.io.StringWriter();
          var pw = new java.io.PrintWriter(sw);
          new Exception().printStackTrace(pw);
          String stackTrace = sw.toString();
          if (!stackTrace.contains("at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed")) {
            System.out.println("Expected non-EDT:");
            System.out.println(stackTrace);
            throw new RuntimeException("Expected EDT: " + stackTrace);
          }
        }
        myController.push(forcePush);
      }

      @Override
      @UIEffect
      public void onSuccess() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
          var sw = new java.io.StringWriter();
          var pw = new java.io.PrintWriter(sw);
          new Exception().printStackTrace(pw);
          String stackTrace = sw.toString();
          System.out.println("Expected EDT:");
          System.out.println(stackTrace);
          throw new RuntimeException("Expected EDT: " + stackTrace);
        }
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
      if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
        var sw = new java.io.StringWriter();
        var pw = new java.io.PrintWriter(sw);
        new Exception().printStackTrace(pw);
        String stackTrace = sw.toString();
        System.out.println("Expected EDT:");
        System.out.println(stackTrace);
        throw new RuntimeException("Expected EDT: " + stackTrace);
      }
      push();
    }
  }
}
