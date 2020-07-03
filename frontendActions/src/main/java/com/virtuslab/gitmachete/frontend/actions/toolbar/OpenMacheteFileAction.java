package com.virtuslab.gitmachete.frontend.actions.toolbar;

import java.util.Collections;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.config.GitVcsSettings;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class OpenMacheteFileAction extends DumbAwareAction implements IExpectsKeyProject {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    Project project = getProject(anActionEvent);

    // When selected Git repository is empty (due to e.g. unopened Git Machete tab)
    // an attempt to guess current repository based on presently opened file
    var gitDir = getSelectedGitRepository(anActionEvent)
        .onEmpty(() -> DvcsUtil.guessCurrentRepositoryQuick(project,
            GitUtil.getRepositoryManager(project),
            GitVcsSettings.getInstance(project).getRecentRootPath()))
        .map(GitVfsUtils::getGitDirectory);

    if (gitDir.isEmpty()) {
      log().warn("Skipping the action because Git repository directory is undefined");
      return;
    }

    var macheteFile = WriteAction.compute(() -> Try
        .of(() -> gitDir.get().findOrCreateChildData(/* requestor */ this, /* name */ "machete"))
        .onFailure(e -> VcsNotifier.getInstance(project).notifyWeakError(
            /* message */ GitMacheteBundle.message("action.open-git-machete-file.notification.fail.cannot-open")))
        .toOption());

    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();

    if (macheteFile.isEmpty()) {
      log().warn("Skipping the action because machete file is undefined");
    } else {
      try {
        doOpenFile(project, macheteFile.get());
      } catch (DirNamedMacheteExistsException e) {
        VcsNotifier.getInstance(project)
            .notifyWeakError(
                /* message */ GitMacheteBundle.message("action.open-git-machete-file.notification.fail.same-name-dir-exists"));
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
