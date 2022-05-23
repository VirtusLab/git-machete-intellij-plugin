package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.common.FastForwardMerge;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;

@CustomLog
public abstract class BaseFastForwardMergeBranchToParentAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToParentStatusDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.BaseFastForwardMergeBranchToParentAction.description");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(SyncToParentStatus.InSync, SyncToParentStatus.InSyncButForkPointOff);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    val stayingBranchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    if (gitRepository == null || stayingBranchName == null) {
      return;
    }

    val stayingBranch = getManagedBranchByName(anActionEvent, stayingBranchName).getOrNull();
    if (stayingBranch == null) {
      return;
    }
    // This is guaranteed by `syncToParentStatusDependentActionUpdate` invoked from `onUpdate`.
    assert stayingBranch.isNonRoot() : "Branch that would be fast-forwarded TO is a root";

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
    val nonRootStayingBranch = stayingBranch.asNonRoot();
    val mergeProps = new MergeProps(
        /* movingBranchName */ nonRootStayingBranch.getParent(),
        /* stayingBranchName */ nonRootStayingBranch);
    FastForwardMerge.perform(currentBranchName, nonRootStayingBranch.getParent().getName(), project, gitRepository, mergeProps);
  }
}
