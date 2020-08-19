package com.virtuslab.gitmachete.frontend.actions.base;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IWithLogger;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.providerservice.BranchLayoutWriterProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;

public abstract class BaseProjectDependentAction extends DumbAwareAction implements IWithLogger {
  @Override
  @UIEffect
  public final void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var maybeProject = anActionEvent.getProject();
    var presentation = anActionEvent.getPresentation();
    if (maybeProject == null) {
      presentation.setEnabledAndVisible(false);
    } else {
      presentation.setEnabledAndVisible(true);
      onUpdate(anActionEvent);
    }
  }

  /**
   * If overridden, {@code super.onUpdate(anActionEvent)} should always be called in the first line of overriding method.
   * In addition, in this method we should use getters WITHOUT logging like
   * {@code IExpectsKeyGitMacheteRepository#getGitMacheteRepositorySnapshotWithoutLogging}
   *
   * @param anActionEvent an action event
   */
  @UIEffect
  protected abstract void onUpdate(AnActionEvent anActionEvent);

  protected Project getProject(AnActionEvent anActionEvent) {
    var project = anActionEvent.getProject();
    assert project != null : "Can't get project from action event";
    return project;
  }

  protected IBranchLayoutWriter getBranchLayoutWriter(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(BranchLayoutWriterProvider.class).getBranchLayoutWriter();
  }

  protected BaseEnhancedGraphTable getGraphTable(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(GraphTableProvider.class).getGraphTable();
  }

  protected Option<GitRepository> getSelectedGitRepositoryWithoutLogging(AnActionEvent anActionEvent) {
    return getProject(anActionEvent).getService(SelectedGitRepositoryProvider.class).getSelectedGitRepository();
  }

  protected Option<GitRepository> getSelectedGitRepositoryWithLogging(AnActionEvent anActionEvent) {
    var gitRepository = getProject(anActionEvent).getService(SelectedGitRepositoryProvider.class)
        .getSelectedGitRepository();
    if (gitRepository.isEmpty()) {
      log().warn("No Git repository is selected");
    }
    return gitRepository;
  }
}
