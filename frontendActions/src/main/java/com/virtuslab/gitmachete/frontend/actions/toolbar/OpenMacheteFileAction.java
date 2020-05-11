package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGraphTable;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;

import java.io.IOException;
import java.util.Collections;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_SELECTED_VCS_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class OpenMacheteFileAction extends DumbAwareAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    Project project = anActionEvent.getProject();

    var gitDir = getSelectedVcsRepository(anActionEvent).flatMap(r -> Option.of(r.getRoot().findChild(".git")));

    if (gitDir.isDefined()) {
      try {
        var macheteFile = gitDir.get().findOrCreateChildData(/* requestor */ this, /* name */ "machete");
        getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
        doOpenFile(project, macheteFile);
      } catch (IOException e) {
        VcsNotifier.getInstance(project).notifyError(/* title */ "", /* message */ "Failed to open machete file");
      } catch (DirNamedMacheteExists dirNamedMacheteExists) {
        VcsNotifier.getInstance(project)
            .notifyError(/* title */ "", /* message */ "Cannot create file 'machete': Directory exists");
      }

    } else {
      LOG.warn("Skipping the action because Git repository is undefined");
    }
  }

  @UIEffect
  private static void doOpenFile(Project project, VirtualFile file) throws DirNamedMacheteExists {
    if (file.isDirectory()) {
      throw new DirNamedMacheteExists();
    }

    openFile(file, project);
  }

  @UIEffect
  public static void openFile(VirtualFile file, Project project) {
    NonProjectFileWritingAccessProvider.allowWriting(Collections.singletonList(file));
    PsiNavigationSupport.getInstance().createNavigatable(project, file, /* offset */ -1).navigate(true);
  }

  private static class DirNamedMacheteExists extends Exception {}
}
