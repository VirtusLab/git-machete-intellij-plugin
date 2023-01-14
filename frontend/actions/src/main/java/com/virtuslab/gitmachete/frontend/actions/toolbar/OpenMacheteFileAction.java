package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitUtil;
import git4idea.config.GitVcsSettings;
import git4idea.repo.GitRepository;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class})
@CustomLog
public class OpenMacheteFileAction extends BaseProjectDependentAction {

  @Override
  protected boolean isSideEffecting() {
    return false;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val project = getProject(anActionEvent);

    // When selected Git repository is empty (due to e.g. unopened Git Machete tab)
    // an attempt to guess current repository based on presently opened file
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val repo = gitRepository != null
        ? gitRepository
        : DvcsUtil.guessCurrentRepositoryQuick(project,
            GitUtil.getRepositoryManager(project),
            GitVcsSettings.getInstance(project).getRecentRootPath());
    val gitDir = repo != null ? repo.getMainGitDirectory() : null;

    if (repo == null || gitDir == null) {
      LOG.warn("Skipping the action because Git repository directory is undefined");
      return;
    }

    val macheteFile = WriteAction.compute(() -> Try
        .of(() -> GitVfsUtils.getMacheteFile(repo))
        .onFailure(e -> VcsNotifier.getInstance(project).notifyWeakError(/* displayId */ null,
            /* title */ "",
            /* message */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.cannot-open"))))
        .getOrNull();

    if (macheteFile == null) {
      val errorWithDiscover = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
          /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.machete-file-not-found"),
          /* message */ getString("action.GitMachete.OpenMacheteFileAction.notification.message.machete-file-not-found")
              .fmt(gitDir.getPath()),
          NotificationType.ERROR);
      // Note there is no `.expire()` call, so the action remains available as long as the notification.
      // It is troublesome to track the notification and cover all cases when it shall be expired.
      // However, the discover action does not any changes directly; a dialog appears with options to accept or cancel.
      // Hence, there is no much harm in leaving this notification without the expiration.
      errorWithDiscover.addAction(NotificationAction
          .createSimple(getString("action.GitMachete.DiscoverAction.GitMacheteToolbar.text"),
              () -> ActionManager.getInstance().getAction(DiscoverAction.class.getSimpleName())
                  .actionPerformed(anActionEvent)));

      VcsNotifier.getInstance(project).notify(errorWithDiscover);

    } else {
      if (macheteFile.isDirectory()) {
        VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
            /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.same-name-dir-exists"),
            /* message */ "");
      }

      OpenFileAction.openFile(macheteFile, project);
    }
  }

  @UIEffect
  public static void openMacheteFile(GitRepository gitRepository) {
    val project = gitRepository.getProject();
    val file = gitRepository.getMacheteFile();
    if (file != null) {
      OpenFileAction.openFile(file, project);
    } else {
      VcsNotifier.getInstance(project).notifyError(
          /* displayId */ null,
          /* title */ getString("action.GitMachete.OpenMacheteFileAction.notification.title.machete-file-not-found"),
          /* message */ getString("action.GitMachete.OpenMacheteFileAction.notification.message.machete-file-not-found")
              .fmt(gitRepository.getRoot().getPath()));
    }
  }
}
