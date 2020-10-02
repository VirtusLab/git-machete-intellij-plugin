package com.virtuslab.gitmachete.frontend.ui.impl.root;

import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@CustomLog
public class RediscoverSuggester extends BaseGitMacheteTabOpenListener {

  private final SelectedGitRepositoryProvider selectedGitRepositoryProvider;

  public RediscoverSuggester(Project project) {
    super(project);
    selectedGitRepositoryProvider = new SelectedGitRepositoryProvider(project);
  }

  @Override
  @UIEffect
  public void perform() {
    var gitRepository = selectedGitRepositoryProvider.getSelectedGitRepository();
    if (gitRepository.isEmpty()) {
      LOG.info("Cannot proceed rediscover suggestion workflow - selected git repository is null");
      return;
    }

    long lastModifiedTimeMillis = 0;
    try {
      lastModifiedTimeMillis = getMacheteFileModificationDate(gitRepository.get());
    } catch (GitMacheteException e) {
      VcsNotifier.getInstance(project).notifyMinorWarning("Failed to get last modification time of the machete file", "");
    }

    var daysDiff = daysDiffTillNow(lastModifiedTimeMillis);
    if (daysDiff > 14) { // magic val
      // t0d0: bundle em all
      var yesNo = MessageDialogBuilder.YesNo.yesNo("Rediscover Suggestion",
          "It looks like you have not modified your git machete file for a while. Would you like to rediscover the branch layout?");
      if (yesNo.show() == Messages.YES) {
        LOG.info("Branch layout has not been modified within given time - scheduling rediscover");
        System.out.println("time for action");
      } else {
        LOG.info("Branch layout has not been modified within given time - rediscover declined from dialog");
      }
    } else {
      LOG.info("Branch layout has been modified within given time - skipping rediscover suggestion");
    }
  }

  private long daysDiffTillNow(long lastModifiedTimeMillis) {
    var currentTimeMillis = System.currentTimeMillis();
    var millisDiff = currentTimeMillis - lastModifiedTimeMillis;
    return millisDiff / (24 * 60 * 60 * 1000);
  }

  private long getMacheteFileModificationDate(GitRepository gitRepository) throws GitMacheteException {
    var readAttrTryOption = Option.of(gitRepository)
        .map(GitVfsUtils::getMacheteFilePath)
        .map(file -> Try.of(() -> Files.readAttributes(file, BasicFileAttributes.class)))
        .map(t -> t.mapTry(attr -> attr.lastModifiedTime().toMillis()));

    if (readAttrTryOption.isDefined()) {
      try {
        return readAttrTryOption.get().getOrElseThrow(e -> new GitMacheteException(e));
      } catch (GitMacheteException e) {
        throw new GitMacheteException("failed to get attrs", e);
      }
    } else {
      throw new GitMacheteException("failed to get attrs");
    }
  }
}
