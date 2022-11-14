package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.GitReference;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.CheckoutBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public abstract class BaseSyncToParentByMergeAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToParentStatusDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseSyncToParentByMergeAction.description-action-name");
  }

  @Override
  public @Untainted @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getNonHtmlString("action.GitMachete.BaseSyncToParentByMergeAction.description");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(SyncToParentStatus.OutOfSync);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU);
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName != null
        ? getManagedBranchByName(anActionEvent, branchName)
        : null;
    val currentBranchNameIfManaged = getCurrentBranchNameIfManaged(anActionEvent);
    val isMergingIntoCurrent = branch != null && currentBranchNameIfManaged != null
        && currentBranchNameIfManaged.equals(branch.getName());
    if (isCalledFromContextMenu && isMergingIntoCurrent) {
      presentation.setText(getString("action.GitMachete.BaseSyncToParentByMergeAction.text"));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val stayingBranchName = getNameOfBranchUnderAction(anActionEvent);
    if (gitRepository == null || stayingBranchName == null) {
      return;
    }

    val movingBranch = getManagedBranchByName(anActionEvent, stayingBranchName);
    if (movingBranch == null) {
      return;
    }
    // This is guaranteed by `syncToParentStatusDependentActionUpdate` invoked from `onUpdate`.
    assert movingBranch.isNonRoot() : "Branch that would be merged INTO is a root";

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    val nonRootMovingBranch = movingBranch.asNonRoot();
    val mergeProps = new MergeProps(
        /* movingBranchName */ nonRootMovingBranch,
        /* stayingBranchName */ nonRootMovingBranch.getParent());

    if (nonRootMovingBranch.getName().equals(currentBranchName)) {
      doMergeIntoCurrentBranch(project, gitRepository, mergeProps);
    } else {
      doMergeIntoNonCurrentBranch(project, gitRepository, mergeProps);
    }
  }

  @UIEffect
  public static void doMergeIntoCurrentBranch(Project project, GitRepository gitRepository, MergeProps mergeProps) {
    val stayingBranch = mergeProps.getStayingBranch().getName();
    LOG.debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}," +
        " stayingBranch = ${stayingBranch}");

    new Task.Backgroundable(project, getString("action.GitMachete.BaseSyncToParentByMergeAction.task-title.current")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        LOG.info("Merging ${stayingBranch} into current branch");

        GitBrancher.getInstance(project)
            .merge(stayingBranch, GitBrancher.DeleteOnMergeOption.NOTHING, Collections.singletonList(gitRepository));
      }
    }.queue();

  }

  @UIEffect
  private void doMergeIntoNonCurrentBranch(Project project, GitRepository gitRepository, MergeProps mergeProps) {
    val stayingBranch = mergeProps.getStayingBranch().getName();
    val movingBranch = mergeProps.getMovingBranch().getName();
    LOG.debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}," +
        " stayingBranch = ${stayingBranch}, movingBranch = ${movingBranch}");

    new Task.Backgroundable(project, getString("action.GitMachete.BaseSyncToParentByMergeAction.task-title.non-current")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        LOG.info("Checking out '${movingBranch}' branch and merging ${stayingBranch} into it");

        CheckoutBackgroundable.doCheckout(project, indicator, movingBranch, gitRepository);
        GitBrancher.getInstance(project)
            .merge(stayingBranch, GitBrancher.DeleteOnMergeOption.NOTHING, Collections.singletonList(gitRepository));
      }

    }.queue();
  }

}
