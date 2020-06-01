package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGraphTable;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getSelectedVcsRepository;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.file.GitVfsUtils;
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

    var gitDir = getSelectedVcsRepository(anActionEvent).map(GitVfsUtils::getGitDirectory);

    if (gitDir.isEmpty()) {
      LOG.warn("Skipping the action because Git repository is undefined");
      return;
    }

    var macheteFile = ApplicationManager.getApplication().runWriteAction(new Computable<Option<VirtualFile>>() {
      @Override
      public Option<VirtualFile> compute() {
        return Try.of(
            () -> Option.of(gitDir.get().findOrCreateChildData(/* requestor */ this, /* name */ "machete")))
            .onFailure(e -> VcsNotifier.getInstance(project)
                .notifyError(/* title */ "", /* message */ "Failed to open machete file"))
            .get();
      }
    });

    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();

    if (macheteFile.isEmpty()) {
      LOG.warn("Skipping the action because machete file is undefined");
    } else {
      try {
        doOpenFile(project, macheteFile.get());
      } catch (DirNamedMacheteExistsException e) {
        VcsNotifier.getInstance(project)
            .notifyError(/* title */ "", /* message */ "Cannot create file 'machete': " +
                "Directory with the same name exists");
      }
    }
  }

  @UIEffect
  private static void doOpenFile(Project project, VirtualFile file) throws DirNamedMacheteExistsException {
    if (file.isDirectory()) {
      throw new DirNamedMacheteExistsException();
    }

    ApplicationManager.getApplication()
        .runWriteAction(() -> NonProjectFileWritingAccessProvider.allowWriting(Collections.singletonList(file)));

    new OpenFileDescriptor(project, file).navigate(/* requestFocus */ true);
  }

  private static class DirNamedMacheteExistsException extends Exception {}
}
