package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;

public abstract class BaseProjectDependentAction extends DumbAwareAction implements IWithLogger {
  @UIEffect
  private boolean isUpdateInProgressOnUIThread;

  @Override
  public boolean isLoggingAcceptable() {
    // We discourage logging while `update()` is in progress since it would lead to massive spam
    // (`update` is invoked very frequently, regardless of whether `actionPerformed` is going to happen).
    return !isUpdateInProgressOnUIThread;
  }

  @Override
  @UIEffect
  public final void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    isUpdateInProgressOnUIThread = true;

    val maybeProject = anActionEvent.getProject();
    val presentation = anActionEvent.getPresentation();
    if (maybeProject == null) {
      presentation.setEnabledAndVisible(false);
    } else {
      presentation.setEnabledAndVisible(true);
      onUpdate(anActionEvent);
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

  protected IBranchLayoutWriter getBranchLayoutWriter() {
    return RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
  }

  protected BaseEnhancedGraphTable getGraphTable(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(GraphTableProvider.class).getGraphTable();
  }

  protected @Nullable GitRepository getSelectedGitRepository(AnActionEvent anActionEvent) {
    val gitRepository = getProject(anActionEvent).getService(SelectedGitRepositoryProvider.class)
        .getSelectedGitRepository();
    if (isLoggingAcceptable() && gitRepository == null) {
      log().warn("No Git repository is selected");
    }
    return gitRepository;
  }
}
