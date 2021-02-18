package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.config.GitVcsSettings;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class OpenMacheteFileAction extends BaseProjectDependentAction {

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
    val gitDir = getSelectedGitRepository(anActionEvent)
        .onEmpty(() -> DvcsUtil.guessCurrentRepositoryQuick(project,
            GitUtil.getRepositoryManager(project),
            GitVcsSettings.getInstance(project).getRecentRootPath()))
        .map(GitVfsUtils::getGitDirectory);

    if (gitDir.isEmpty()) {
      log().warn("Skipping the action because Git repository directory is undefined");
      return;
    }

    val macheteFile = WriteAction.compute(() -> Try
        .of(() -> gitDir.get().findOrCreateChildData(/* requestor */ this, /* name */ "machete"))
        .onFailure(e -> VcsNotifier.getInstance(project).notifyWeakError(
            /* message */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.cannot-open")))
        .toOption());

    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();

    if (macheteFile.isEmpty()) {
      VcsNotifier.getInstance(project).notifyError(
          /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.machete-file-not-found"),
          /* message */ format(getString("action.GitMachete.OpenMacheteFileAction.notification.message.machete-file-not-found"),
              gitDir.get().getPath()));
    } else {
      VirtualFile file = macheteFile.get();
      if (file.isDirectory()) {
        VcsNotifier.getInstance(project).notifyError(
            /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.same-name-dir-exists"),
            /* message */ "");
      }

      OpenFileAction.openFile(file, project);
    }
  }
}
