package com.virtuslab.gitmachete.frontend.actions.pushdialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.Border;

import com.intellij.dvcs.push.PrePushHandler;
import com.intellij.dvcs.push.PushController;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.dvcs.push.PushSource;
import com.intellij.dvcs.push.PushSupport;
import com.intellij.dvcs.push.PushTarget;
import com.intellij.dvcs.push.VcsPushOptionValue;
import com.intellij.dvcs.push.ui.PushLog;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.push.ui.VcsPushUi;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import io.vavr.collection.List;
import net.miginfocom.swing.MigLayout;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GitPushDialog extends DialogWrapper implements VcsPushUi {
  private static final String DIMENSION_KEY = "Vcs.Push.Dialog.v2";

  private static final int CENTER_PANEL_HEIGHT = 450;
  private static final int CENTER_PANEL_WIDTH = 800;

  private final boolean isForcePushRequired;
  private final Project project;
  private final PushController pushController;
  private final PushLog listPanel;
  private final Action pushAction;

  @UIEffect
  public GitPushDialog(
      Project project,
      List<? extends Repository> selectedRepositories,
      PushSource pushSource,
      boolean isForcePushRequired) {
    super(project, /* canBeParent */ true,
        Registry.is("ide.perProjectModality") ? IdeModalityType.PROJECT : IdeModalityType.IDE);
    this.isForcePushRequired = isForcePushRequired;
    this.project = project;

    @SuppressWarnings("nullness:assignment.type.incompatible") @Initialized MyVcsPushDialog dialog = new MyVcsPushDialog(this,
        project, selectedRepositories.asJava(), pushSource);
    this.pushController = dialog.getPushController();
    this.listPanel = pushController.getPushPanelLog();

    @SuppressWarnings("nullness:assignment.type.incompatible") @Initialized Action action = new PushSwingAction(this,
        isForcePushRequired);
    this.pushAction = action;

    // Note: since the class is final, `this` is already @Initialized at this point.

    dialog.updateOkActions();
    setOKButtonText(getPushActionName(isForcePushRequired));
    setTitle("Push Commits");
    init();
  }

  private static class MyVcsPushDialog extends VcsPushDialog {

    private final @NotOnlyInitialized GitPushDialog outer;

    @UIEffect
    MyVcsPushDialog(
        @UnderInitialization GitPushDialog outer,
        Project project,
        java.util.List<? extends Repository> selectedRepositories,
        PushSource pushSource) {
      // Presented dialog shows commits for branches belonging to allRepositories, preselectedRepositories and currentRepo.
      // The second and the third one have higher priority of loading its commits.
      // From our perspective, we always have single (pre-selected) repository so we do not care about the priority.
      super(project, selectedRepositories, selectedRepositories, /* currentRepo */ null, pushSource);
      this.outer = outer;
    }

    public PushController getPushController() {
      return myController;
    }

    @Override
    @UIEffect
    public void updateOkActions() {
      super.updateOkActions();
    }

    @Override
    @UIEffect
    public @Nullable VcsPushOptionValue getAdditionalOptionValue(PushSupport support) {
      return outer.getAdditionalOptionValue(support);
    }

    @Override
    @UIEffect
    public JRootPane getRootPane() {
      return outer.getRootPane();
    }
  }

  @Override
  protected @Nullable Border createContentPaneBorder() {
    return null;
  }

  @Override
  @UIEffect
  protected JPanel createSouthAdditionalPanel() {
    return createSouthOptionsPanel();
  }

  @Override
  @UIEffect
  protected JComponent createSouthPanel() {
    JComponent southPanel = super.createSouthPanel();
    southPanel.setBorder(JBUI.Borders.empty(/* topAndBottom */ 8, /* leftAndRight */ 12));
    return southPanel;
  }

  @Override
  @UIEffect
  protected JComponent createCenterPanel() {
    JPanel panel = JBUI.Panels.simplePanel(/* hgap */ 0, /* vgap */ 2)
        .addToCenter(listPanel)
        .addToBottom(createOptionsPanel());
    listPanel.setPreferredSize(new JBDimension(CENTER_PANEL_WIDTH, CENTER_PANEL_HEIGHT));
    return panel;
  }

  protected JPanel createOptionsPanel() {
    return new JPanel(new MigLayout(/* layoutConstraints */ "ins 0 0, flowy")) {
      @Override
      public Component add(Component comp) {
        JPanel wrapperPanel = new BorderLayoutPanel().addToCenter(comp);
        wrapperPanel.setBorder(JBUI.Borders.empty(5, 15, 0, 0));
        return super.add(wrapperPanel);
      }
    };
  }

  @UIEffect
  private JPanel createSouthOptionsPanel() {
    return new JPanel(new MigLayout(/* layoutConstraints */ "ins 0 ${JBUI.scale(20)}px 0 0, flowx, gapx ${JBUI.scale(16)}px"));
  }

  @Override
  protected String getDimensionServiceKey() {
    return DIMENSION_KEY;
  }

  @Override
  @UIEffect
  public JRootPane getRootPane() {
    return super.getRootPane();
  }

  @Override
  @UIEffect
  protected void doOKAction() {
    push(isForcePushRequired);
  }

  @Override
  @UIEffect
  protected Action[] createActions() {
    return new Action[]{pushAction, getCancelAction()};
  }

  @Override
  @UIEffect
  public boolean canPush() {
    return pushController.isPushAllowed();
  }

  @Override
  @UIEffect
  public java.util.Map<PushSupport<Repository, PushSource, PushTarget>, java.util.Collection<PushInfo>> getSelectedPushSpecs() {
    return pushController.getSelectedPushSpecs();
  }

  @Override
  @UIEffect
  public @Nullable JComponent getPreferredFocusedComponent() {
    return listPanel.getPreferredFocusedComponent();
  }

  @Override
  protected Action getOKAction() {
    return pushAction;
  }

  @Override
  @UIEffect
  public void push(boolean forcePush) {
    executeAfterRunningPrePushHandlers(
        new Task.Backgroundable(project, /* title */ "Pushing...", /* canBeCancelled */ true) {
          @Override
          public void run(ProgressIndicator indicator) {
            pushController.push(forcePush);
          }
        });
  }

  @Override
  @UIEffect
  public void executeAfterRunningPrePushHandlers(Task.Backgroundable activity) {
    PrePushHandler.Result result = runPrePushHandlersInModalTask();
    if (result == PrePushHandler.Result.OK) {
      activity.queue();
      close(OK_EXIT_CODE);
    } else if (result == PrePushHandler.Result.ABORT_AND_CLOSE) {
      doCancelAction();
    } else if (result == PrePushHandler.Result.ABORT) {
      // cancel push and leave the push dialog open
    }
  }

  @UIEffect
  public PrePushHandler.Result runPrePushHandlersInModalTask() {
    FileDocumentManager.getInstance().saveAllDocuments();
    AtomicReference<PrePushHandler.Result> result = new AtomicReference<>(PrePushHandler.Result.OK);
    new Task.Modal(project, /* title */ "Checking Commits...", /* canBeCancelled */ true) {
      @Override
      public void run(ProgressIndicator indicator) {
        result.set(pushController.executeHandlers(indicator));
      }

      @Override
      @UIEffect
      public void onThrowable(Throwable error) {
        if (error instanceof PushController.HandlerException) {
          PushController.HandlerException handlerException = (PushController.HandlerException) error;
          Throwable cause = handlerException.getCause();

          String failedHandler = handlerException.getFailedHandlerName();
          java.util.List<String> skippedHandlers = handlerException.getSkippedHandlers();

          String suggestionMessageProblem;
          if (cause instanceof ProcessCanceledException) {
            suggestionMessageProblem = "${failedHandler} has been cancelled.";
          } else {
            // PushController.HandlerException constructor guarantees that cause is not null
            assert cause != null : "Exception cause is null";
            super.onThrowable(cause);
            suggestionMessageProblem = "${failedHandler} has failed. See log for more details.";
          }

          String suggestionMessageQuestion = skippedHandlers.isEmpty()
              ? "Would you like to push anyway or cancel the push completely?"
              : "Would you like to skip all remaining pre-push steps and push anyway, or cancel the push completely?";

          suggestToSkipOrPush(suggestionMessageProblem + System.lineSeparator() + suggestionMessageQuestion);
        } else {
          super.onThrowable(error);
        }
      }

      @Override
      @UIEffect
      public void onCancel() {
        super.onCancel();
        suggestToSkipOrPush("Would you like to skip all pre-push steps and push, or cancel the push completely?");
      }

      @UIEffect
      private void suggestToSkipOrPush(String message) {
        if (Messages.showOkCancelDialog(project,
            message,
            /* title */ "Push",
            /* okText */ "Push Anyway",
            /* cancelText */ "Cancel",
            UIUtil.getWarningIcon()) == Messages.OK) {
          result.set(PrePushHandler.Result.OK);
        } else {
          result.set(PrePushHandler.Result.ABORT);
        }
      }
    }.queue();

    PrePushHandler.Result resultValue = result.get();
    assert resultValue != null : "Result value is null";
    return resultValue;
  }

  @Override
  public @Nullable VcsPushOptionValue getAdditionalOptionValue(PushSupport support) {
    return null;
  }

  private static String getPushActionName(boolean isForcePushRequired) {
    return isForcePushRequired ? "Force _Push" : "_Push";
  }

  private static final class PushSwingAction extends AbstractAction {

    private final @NotOnlyInitialized VcsPushUi vcsPushUi;
    private final boolean isForcePushRequired;

    @UIEffect
    private PushSwingAction(@UnderInitialization VcsPushUi vcsPushUi, boolean isForcePushRequired) {
      super(getPushActionName(isForcePushRequired));
      this.vcsPushUi = vcsPushUi;
      this.isForcePushRequired = isForcePushRequired;

      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    @UIEffect
    public void actionPerformed(ActionEvent e) {
      vcsPushUi.push(/* forcePush */ isForcePushRequired);
    }
  }
}
