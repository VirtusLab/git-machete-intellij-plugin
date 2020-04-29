package com.virtuslab.gitmachete.frontend.actions.toolbar;

import java.util.Objects;
import java.util.function.Function;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.common.ActionUtils;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 *  <li>{@link CommonDataKeys#EDITOR}</li>
 *  <li>{@link CommonDataKeys#VIRTUAL_FILE_ARRAY}</li>
 * </ul>
 */
public class PushAction extends AnAction implements DumbAware {

  @Override
  @UIEffect
  public void update(AnActionEvent e) {
    super.update(e);
    Project project = e.getProject();
    e.getPresentation()
        .setEnabledAndVisible(project != null && !VcsRepositoryManager.getInstance(project).getRepositories().isEmpty());
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    Project project = ActionUtils.getProject(anActionEvent);
    VcsRepositoryManager manager = VcsRepositoryManager.getInstance(project);

    boolean isEditorActive = anActionEvent.getData(CommonDataKeys.EDITOR) != null;
    List<Repository> repositories = isEditorActive
        ? List.empty()
        : collectIdeaRepositories(manager, anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));

    VirtualFile selectedFile = DvcsUtil.getSelectedFile(project);
    Repository repositoryToSelect = selectedFile != null ? manager.getRepositoryForFileQuick(selectedFile) : null;

    new VcsPushDialog(project, DvcsUtil.sortRepositories(repositories.asJava()), repositoryToSelect).show();
  }

  private static List<Repository> collectIdeaRepositories(VcsRepositoryManager vcsRepositoryManager, VirtualFile[] files) {
    if (files == null) {
      return List.empty();
    }

    Function<VirtualFile, @Nullable Repository> fileToRepository = vcsRepositoryManager::getRepositoryForFileQuick;
    return List.of(files)
        .map(fileToRepository)
        .filter(Objects::nonNull)
        .collect(List.collector());
  }
}
