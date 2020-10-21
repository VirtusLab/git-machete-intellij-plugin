package com.virtuslab.gitmachete.frontend.ui.impl.root;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@CustomLog
public class RediscoverSuggester extends BaseGitMacheteTabOpenListener {

  private final SelectedGitRepositoryProvider selectedGitRepositoryProvider;

  // TODO (#270): a candidate for custom settings tab
  private final int DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER = 14;

  private boolean wasRediscoverSuggestionDeclined = false;

  public RediscoverSuggester(Project project) {
    super(project);
    selectedGitRepositoryProvider = new SelectedGitRepositoryProvider(project);
  }

  @Override
  @UIEffect
  public void perform() {
    if (wasRediscoverSuggestionDeclined) {
      return;
    }

    var gitRepository = selectedGitRepositoryProvider.getSelectedGitRepository();
    if (gitRepository.isEmpty()) {
      LOG.warn("Cannot proceed rediscover suggestion workflow - selected git repository is null");
      return;
    }

    var macheteFilePath = Option.of(gitRepository.get()).map(GitVfsUtils::getMacheteFilePath).getOrNull();
    if (macheteFilePath == null) {
      LOG.warn("Cannot proceed rediscover suggestion workflow - selected machete file is null");
      return;
    }

    var lastModifiedTimeMillis = getFileModificationDate(macheteFilePath).getOrNull();
    if (lastModifiedTimeMillis == null) {
      LOG.warn("Cannot proceed rediscover suggestion workflow - could not get file modification date");
      return;
    }

    var daysDiff = daysDiffTillNow(lastModifiedTimeMillis);
    LOG.info("Branch layout has not been modified within ${daysDiff} days");
    if (daysDiff > DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER) {
      LOG.info("Time diff above ${DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER}; Suggesting rediscover");
      var yesNo = MessageDialogBuilder.YesNo.yesNo(
          getString("string.GitMachete.RediscoverSuggester.dialog.title"),
          getString("string.GitMachete.RediscoverSuggester.dialog.question"));

      switch (yesNo.show()) {
        case Messages.YES :
          LOG.info("Enqueueing rediscover");
          new GraphTableProvider(project).getGraphTable().queueDiscover(macheteFilePath, () -> {});
          break;
        case Messages.NO : // closing dialog goes here too
          LOG.info("Rediscover declined from dialog");
          wasRediscoverSuggestionDeclined = true;
          break;
        default :
          LOG.info("Unknown response message");
          break;
      }
    } else {
      LOG.info("Time diff below (or equal) ${DAYS_AFTER_WHICH_TO_SUGGEST_DISCOVER}; rediscover suggestion skipped");
    }
  }

  private long daysDiffTillNow(long lastModifiedTimeMillis) {
    var currentTimeMillis = System.currentTimeMillis();
    var millisDiff = currentTimeMillis - lastModifiedTimeMillis;
    return millisDiff / (24 * 60 * 60 * 1000);
  }

  private Option<Long> getFileModificationDate(Path filePath) {
    return Try.of(() -> Files.readAttributes(filePath, BasicFileAttributes.class))
        .map(attr -> attr.lastModifiedTime().toMillis()).toOption();
  }
}
