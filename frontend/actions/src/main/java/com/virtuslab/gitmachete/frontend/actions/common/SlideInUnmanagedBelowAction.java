package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInNonRootBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInRootBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyUnmanagedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class SlideInUnmanagedBelowAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeyGitMacheteRepository,
      IExpectsKeySelectedBranchName,
      IExpectsKeyUnmanagedBranchName {

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val parentName = getSelectedBranchName(anActionEvent);
    val unmanagedBranch = getNameOfUnmanagedBranch(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);
    val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);

    if (gitRepository == null || branchLayout == null || unmanagedBranch == null) {
      return;
    }

    val slideInOptions = new SlideInOptions(unmanagedBranch, /* shouldReattach */ false, /* customAnnotation */ "");

    Runnable preSlideInRunnable = () -> {};

    val parentEntry = parentName != null ? branchLayout.getEntryByName(parentName) : null;
    val entryAlreadyExistsBelowGivenParent = parentEntry != null
        && parentEntry.getChildren().map(BranchLayoutEntry::getName)
            .map(names -> names.contains(slideInOptions.getName()))
            .getOrElse(false);

    if (entryAlreadyExistsBelowGivenParent && slideInOptions.shouldReattach()) {
      LOG.debug("Skipping action: Branch layout entry already exists below given parent");
      return;
    }

    val graphTable = getGraphTable(anActionEvent);
    if (parentName != null) {
      new SlideInNonRootBackgroundable(
          gitRepository,
          branchLayout,
          branchLayoutWriter,
          graphTable,
          preSlideInRunnable,
          slideInOptions,
          parentName).queue();
    } else {

      new SlideInRootBackgroundable(
          gitRepository,
          branchLayout,
          branchLayoutWriter,
          graphTable,
          preSlideInRunnable,
          slideInOptions).queue();
    }
  }
}
