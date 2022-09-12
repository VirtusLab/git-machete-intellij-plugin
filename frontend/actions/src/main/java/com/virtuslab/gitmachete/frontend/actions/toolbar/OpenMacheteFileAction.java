package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.config.GitVcsSettings;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class OpenMacheteFileAction extends BaseProjectDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    final var project = getProject(anActionEvent);

    // When selected Git repository is empty (due to e.g. unopened Git Machete tab)
    // an attempt to guess current repository based on presently opened file
    final var selectedGitRepo = getSelectedGitRepository(anActionEvent);
    final var repo = selectedGitRepo != null
        ? selectedGitRepo
        : DvcsUtil.guessCurrentRepositoryQuick(project,
            GitUtil.getRepositoryManager(project),
            GitVcsSettings.getInstance(project).getRecentRootPath());
    final var gitDir = repo != null ? GitVfsUtils.getMainGitDirectory(repo) : null;

    if (gitDir == null) {
      log().warn("Skipping the action because Git repository directory is undefined");
      return;
    }

    final var macheteFile = WriteAction.compute(() -> Try
        .of(() -> gitDir.findOrCreateChildData(/* requestor */ this, /* name */ "machete"))
        .onFailure(e -> VcsNotifier.getInstance(project).notifyWeakError(/* displayId */ null,
            /* title */ "",
            /* message */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.cannot-open")))
        .toOption());

    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();

    if (macheteFile.isEmpty()) {
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.machete-file-not-found"),
          /* message */ getString("action.GitMachete.OpenMacheteFileAction.notification.message.machete-file-not-found")
              .format(gitDir.getPath()));
    } else {
      VirtualFile file = macheteFile.get();
      if (file.isDirectory()) {
        VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
            /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.same-name-dir-exists"),
            /* message */ "");
      }

      OpenFileAction.openFile(file, project);
    }
  }
}
