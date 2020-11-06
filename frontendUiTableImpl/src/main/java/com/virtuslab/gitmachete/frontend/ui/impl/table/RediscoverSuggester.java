package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getFileModificationDate;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.setFileModificationDate;

import java.nio.file.Path;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.GuiUtils;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@CustomLog
@RequiredArgsConstructor
public class RediscoverSuggester {

  private final Project project;

  private final GitRepository gitRepository;

  private final Runnable discoverOperation;

  // TODO (#270): a candidate for custom settings tab
  private final int DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER = 14;

  public void performIfNotDeclined() {
    var macheteFilePath = Option.of(gitRepository).map(GitVfsUtils::getMacheteFilePath).getOrNull();
    if (macheteFilePath == null) {
      LOG.warn("Cannot proceed with rediscover suggestion workflow - selected machete file is null");
      return;
    }

    var lastModifiedTimeMillis = getFileModificationDate(macheteFilePath).getOrNull();
    if (lastModifiedTimeMillis == null) {
      LOG.warn("Cannot proceed with rediscover suggestion workflow - could not get file modification date");
      return;
    }

    var daysDiff = daysDiffTillNow(lastModifiedTimeMillis);
    LOG.info("Branch layout has not been modified within ${daysDiff} days");
    if (daysDiff > DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER) {
      LOG.info("Time diff above ${DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER}; Suggesting rediscover");
      queueSuggestion(macheteFilePath);
    } else {
      LOG.info("Time diff below (or equal) ${DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER}; rediscover suggestion skipped");
    }
  }

  private void queueSuggestion(Path macheteFilePath) {
    var yesNo = MessageDialogBuilder.YesNo.yesNo(
        getString("string.GitMachete.RediscoverSuggester.dialog.title"),
        getString("string.GitMachete.RediscoverSuggester.dialog.question"));

    new Task.Backgroundable(project, getString("string.GitMachete.RediscoverSuggester.task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        GuiUtils.invokeLaterIfNeeded(() -> {
          switch (yesNo.show()) {
            case Messages.YES :
              LOG.info("Enqueueing rediscover");
              discoverOperation.run();
              break;
            case Messages.NO : // closing dialog goes here too
              LOG.info("Rediscover declined from dialog");
              setFileModificationDate(macheteFilePath, System.currentTimeMillis());
              break;
            default :
              LOG.info("Unknown response message");
              break;
          }
        }, ModalityState.NON_MODAL);
      }
    }.queue();
  }

  private long daysDiffTillNow(long lastModifiedTimeMillis) {
    var currentTimeMillis = System.currentTimeMillis();
    var millisDiff = currentTimeMillis - lastModifiedTimeMillis;
    return millisDiff / (24 * 60 * 60 * 1000);
  }
}
