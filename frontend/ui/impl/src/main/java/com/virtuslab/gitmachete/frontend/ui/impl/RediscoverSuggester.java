package com.virtuslab.gitmachete.frontend.ui.impl;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;

import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod(GitVfsUtils.class)
@CustomLog
@RequiredArgsConstructor
public class RediscoverSuggester {

  private final GitRepository gitRepository;

  private final Runnable queueDiscoverOperation;

  // TODO (#270): a candidate for custom settings tab
  private final int DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER = 14;

  @UIEffect
  public void perform() {
    val macheteFilePath = Option.of(gitRepository).map(GitVfsUtils::getMacheteFilePath).getOrNull();
    if (macheteFilePath == null) {
      LOG.warn("Cannot proceed with rediscover suggestion workflow - selected machete file is null");
      return;
    }

    val lastModifiedTimeMillis = macheteFilePath.getFileModificationDate().getOrNull();
    if (lastModifiedTimeMillis == null) {
      LOG.warn("Cannot proceed with rediscover suggestion workflow - could not get file modification date");
      return;
    }

    val daysDiff = daysDiffTillNow(lastModifiedTimeMillis);
    LOG.info("Branch layout has not been modified within ${daysDiff} days");
    if (daysDiff > DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER) {
      LOG.info("Time diff above ${DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER}; Suggesting rediscover");
      queueSuggestion(macheteFilePath);
    } else {
      LOG.info("Time diff below (or equal) ${DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER}; rediscover suggestion skipped");
    }
  }

  @UIEffect
  private void queueSuggestion(Path macheteFilePath) {
    val yesNo = MessageDialogBuilder.yesNo(
        getString("string.GitMachete.RediscoverSuggester.dialog.title"),
        getString("string.GitMachete.RediscoverSuggester.dialog.question"));

    switch (yesNo.show()) {
      case Messages.YES :
        LOG.info("Enqueueing rediscover");
        queueDiscoverOperation.run();
        break;
      case Messages.NO : // closing dialog goes here too
        LOG.info("Rediscover declined from dialog");
        macheteFilePath.setFileModificationDate(System.currentTimeMillis());
        break;
      default :
        LOG.info("Unknown response message");
        break;
    }
  }

  private long daysDiffTillNow(long lastModifiedTimeMillis) {
    val currentTimeMillis = System.currentTimeMillis();
    val millisDiff = currentTimeMillis - lastModifiedTimeMillis;
    return millisDiff / (24 * 60 * 60 * 1000);
  }
}
