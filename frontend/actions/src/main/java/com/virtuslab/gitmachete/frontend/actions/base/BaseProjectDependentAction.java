package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.common.SideEffectingActionTrackingService;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;

@ExtensionMethod({GitMacheteBundle.class})
public abstract class BaseProjectDependentAction extends DumbAwareAction implements IWithLogger {
  @UIEffect
  private boolean isUpdateInProgressOnUIThread;

  @Override
  public boolean isLoggingAcceptable() {
    // We discourage logging while `update()` is in progress since it would lead to massive spam
    // (`update` is invoked very frequently, regardless of whether `actionPerformed` is going to happen).
    return !isUpdateInProgressOnUIThread;
  }

  protected abstract boolean isSideEffecting();

  @SuppressWarnings("tainting:return")
  protected static @Nullable @Untainted String getOngoingSideEffectingActions(Project project) {
    val actions = project.getService(SideEffectingActionTrackingService.class).getOngoingActions();
    if (actions.isEmpty()) {
      return null;
    }
    if (actions.size() == 1) {
      return actions.head();
    }
    val sorted = actions.toList();
    return sorted.init().mkString(", ") + " and " + sorted.last();
  }

  @Override
  @UIEffect
  public final void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    isUpdateInProgressOnUIThread = true;

    val project = anActionEvent.getProject();
    val presentation = anActionEvent.getPresentation();
    if (project == null) {
      presentation.setEnabledAndVisible(false);
    } else {
      val ongoingSideEffectingActions = getOngoingSideEffectingActions(project);
      if (isSideEffecting() && ongoingSideEffectingActions != null) {
        // Note that we still need to call onUpdate to decide whether the action should remain visible.
        // At the start of each `update()` call, presentation is apparently always set to visible;
        // let's still call setEnabledAndVisible(true) to ensure a consistent behavior.
        presentation.setEnabledAndVisible(true);
        onUpdate(anActionEvent);
        presentation.setEnabled(false);
        presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.another-actions-ongoing")
            .fmt(ongoingSideEffectingActions));
      } else {
        presentation.setEnabledAndVisible(true);
        onUpdate(anActionEvent);
      }
    }

    isUpdateInProgressOnUIThread = false;
  }

  /**
   * If overridden, {@code super.onUpdate(anActionEvent)} should always be called in the first line of overriding method.
   *
   * @param anActionEvent an action event
   */
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {}

  @Override
  public abstract LambdaLogger log();

  protected Project getProject(AnActionEvent anActionEvent) {
    val project = anActionEvent.getProject();
    assert project != null : "Can't get project from action event";
    return project;
  }

  protected BaseEnhancedGraphTable getGraphTable(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(BaseEnhancedGraphTable.class);
  }

  protected @Nullable GitRepository getSelectedGitRepository(AnActionEvent anActionEvent) {
    val gitRepository = getProject(anActionEvent).getService(IGitRepositorySelectionProvider.class)
        .getSelectedGitRepository();
    if (isLoggingAcceptable() && gitRepository == null) {
      log().warn("No Git repository is selected");
    }
    return gitRepository;
  }
}
