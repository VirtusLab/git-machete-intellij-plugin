package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInNonRootBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideInRootBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyUnmanagedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class SlideInUnmanagedBelowAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeyGitMacheteRepository,
      IExpectsKeySelectedBranchName,
      IExpectsKeyUnmanagedBranchName {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val parentName = getSelectedBranchName(anActionEvent);
    val unmanagedBranch = getNameOfUnmanagedBranch(anActionEvent);
    val branchLayout = getBranchLayout(anActionEvent);
    val branchLayoutWriter = getBranchLayoutWriter();

    if (gitRepository == null || branchLayout == null || unmanagedBranch == null) {
      return;
    }

    val slideInOptions = new SlideInOptions(unmanagedBranch, /* shouldReattach */ false);

    Runnable preSlideInRunnable = () -> {};

    val parentEntry = parentName != null ? branchLayout.getEntryByName(parentName) : null;
    val entryAlreadyExistsBelowGivenParent = parentEntry != null
        && parentEntry.getChildren().map(BranchLayoutEntry::getName)
            .map(names -> names.contains(slideInOptions.getName()))
            .getOrElse(false);

    if (entryAlreadyExistsBelowGivenParent && slideInOptions.shouldReattach()) {
      log().debug("Skipping action: Branch layout entry already exists below given parent");
      return;
    }

    if (parentName != null) {
      new SlideInNonRootBackgroundable(project,
          gitRepository,
          branchLayout,
          branchLayoutWriter,
          preSlideInRunnable,
          slideInOptions,
          parentName) {
        @Override
        public void onFinished() {
          getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
        }
      }.queue();
    } else {

      new SlideInRootBackgroundable(project,
          gitRepository,
          branchLayout,
          branchLayoutWriter,
          preSlideInRunnable,
          slideInOptions) {
        @Override
        public void onFinished() {
          getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
        }
      }.queue();
    }
  }
}
