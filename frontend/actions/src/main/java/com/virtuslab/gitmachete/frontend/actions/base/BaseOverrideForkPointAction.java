package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.config.GitConfigUtil;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.OverrideForkPointDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public abstract class BaseOverrideForkPointAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      ISyncToParentStatusDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseOverrideForkPointAction.description-action-name");
  }

  @Override
  public @Untainted @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getNonHtmlString("action.GitMachete.BaseOverrideForkPointAction.description");
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
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val branchUnderAction = getNameOfBranchUnderAction(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchUnderAction);

    if (gitRepository == null || branch == null || branch.isRoot()) {
      return;
    }

    val nonRootBranch = branch.asNonRoot();
    val selectedCommit = new OverrideForkPointDialog(project, nonRootBranch).showAndGetSelectedCommit();
    if (selectedCommit == null) {
      log().debug(
          "Commit selected to be the new fork point is null: most likely the action has been canceled from override-fork-point dialog");
      return;
    }

    LOG.debug("Enqueueing fork point override");
    new Task.Backgroundable(project, "Overriding fork point...") {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        overrideForkPoint(anActionEvent, branch, selectedCommit);
      }
    }.queue();
  }

  @UIThreadUnsafe
  private void overrideForkPoint(AnActionEvent anActionEvent, IManagedBranchSnapshot branch, ICommitOfManagedBranch forkPoint) {
    val gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository != null) {
      val root = gitRepository.getRoot();
      val project = getProject(anActionEvent);
      setOverrideForkPointConfigValues(project, root, branch.getName(), forkPoint, branch.getPointedCommit());
    }

    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  @UIThreadUnsafe
  private void setOverrideForkPointConfigValues(
      Project project,
      VirtualFile root,
      String branchName,
      ICommitOfManagedBranch forkPoint,
      ICommitOfManagedBranch ancestorCommit) {
    val section = "machete";
    val subsectionPrefix = "overrideForkPoint";
    val to = "to";
    val whileDescendantOf = "whileDescendantOf";

    // Section spans the characters before the first dot
    // Name spans the characters after the last dot
    // Subsection is everything else
    val toKey = "${section}.${subsectionPrefix}.${branchName}.${to}";
    val whileDescendantOfKey = "${section}.${subsectionPrefix}.${branchName}.${whileDescendantOf}";

    try {
      GitConfigUtil.setValue(project, root, toKey, forkPoint.getHash());
    } catch (VcsException e) {
      LOG.info("Attempt to set '${toKey}' git config value failed: " + e.getMessage());
    }

    try {
      GitConfigUtil.setValue(project, root, whileDescendantOfKey, forkPoint.getHash());
    } catch (VcsException e) {
      LOG.info("Attempt to get '${whileDescendantOf}' git config value failed: " + e.getMessage());
    }
  }
}
