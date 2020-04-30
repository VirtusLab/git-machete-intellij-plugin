package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 * </ul>
 */
public class PushAction extends AnAction implements DumbAware {

  @Override
  @UIEffect
  public void update(AnActionEvent e) {
    super.update(e);

    Project project = e.getProject();
    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(e);

    boolean isSomeRepositoryPresent = project != null && !VcsRepositoryManager.getInstance(project).getRepositories().isEmpty();
    boolean isSelectedVcsRepositoryPresent = selectedVcsRepository.isDefined();

    e.getPresentation().setEnabledAndVisible(isSomeRepositoryPresent && isSelectedVcsRepositoryPresent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    Project project = ActionUtils.getProject(anActionEvent);

    Option<GitRepository> selectedVcsRepository = ActionUtils.getSelectedVcsRepository(anActionEvent);
    if (selectedVcsRepository.isDefined()) {
      new VcsPushDialog(project, selectedVcsRepository.toJavaList(), /* currentRepo */ null).show();
    }
  }
}
