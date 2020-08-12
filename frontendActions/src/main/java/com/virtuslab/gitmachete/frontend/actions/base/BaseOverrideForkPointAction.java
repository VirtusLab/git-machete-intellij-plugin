package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.config.GitConfigUtil;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.dialogs.OverrideForkPointDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;
import com.virtuslab.qual.guieffect.NotUIThreadSafe;

@CustomLog
public abstract class BaseOverrideForkPointAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeyProject,
      IExpectsKeyGitMacheteRepository,
      ISyncToParentStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseOverrideForkPointAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.BaseOverrideForkPointAction.description");
  }

  @Override
  public @I18nFormat({}) String getActionTextForCurrentBranch() {
    return getString("action.GitMachete.BaseOverrideForkPointAction.text.current-branch");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(SyncToParentStatus.InSyncButForkPointOff);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var branchUnderAction = getNameOfBranchUnderAction(anActionEvent);
    var branch = branchUnderAction.flatMap(pn -> getGitMacheteBranchByName(anActionEvent, pn)).getOrNull();

    if (gitRepository == null || branch == null || branch.isRootBranch()) {
      return;
    }

    var nonRootBranch = branch.asNonRootBranch();
    var selectedCommit = new OverrideForkPointDialog(project, nonRootBranch.getParentBranch(), nonRootBranch)
        .showAndGetSelectedCommit();
    if (selectedCommit == null) {
      log().debug(
          "Commit selected to be the new fork point is null: most likely the action has been canceled from override-fork-point dialog");
      return;
    }

    LOG.debug("Enqueueing fork point override");
    new Task.Backgroundable(project, "Overriding fork point...") {
      @Override
      public void run(ProgressIndicator indicator) {
        overrideForkPoint(anActionEvent, branch, selectedCommit);
      }
    }.queue();
  }

  @NotUIThreadSafe
  private void overrideForkPoint(AnActionEvent anActionEvent, IGitMacheteBranch branch, IGitMacheteCommit forkPoint) {
    var gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      var root = gitRepository.get().getRoot();
      var project = getProject(anActionEvent);
      setOverrideForkPointConfigValues(project, root, branch.getName(), forkPoint, branch.getPointedCommit());
    }

    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  @NotUIThreadSafe
  private void setOverrideForkPointConfigValues(
      Project project,
      VirtualFile root,
      String branchName,
      IGitMacheteCommit forkPoint,
      IGitMacheteCommit ancestorCommit) {
    var section = "machete";
    var subsectionPrefix = "overrideForkPoint";
    var to = "to";
    var whileDescendantOf = "whileDescendantOf";

    // Section spans the characters before the first dot
    // Name spans the characters after the last dot
    // Subsection is everything else
    var toKey = "${section}.${subsectionPrefix}.${branchName}.${to}";
    var whileDescendantOfKey = "${section}.${subsectionPrefix}.${branchName}.${whileDescendantOf}";

    try {
      GitConfigUtil.setValue(project, root, toKey, forkPoint.getHash());
    } catch (VcsException e) {
      LOG.info("Attempt to set '${toKey}' git config value failed: " + e.getMessage());
    }

    try {
      GitConfigUtil.setValue(project, root, whileDescendantOfKey, ancestorCommit.getHash());
    } catch (VcsException e) {
      LOG.info("Attempt to get '${whileDescendantOf}' git config value failed: " + e.getMessage());
    }
  }
}
