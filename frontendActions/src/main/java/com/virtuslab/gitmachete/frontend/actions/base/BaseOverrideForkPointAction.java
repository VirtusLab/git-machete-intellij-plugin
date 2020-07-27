package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static java.text.MessageFormat.format;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.config.GitConfigUtil;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.frontend.actions.dialogs.OverrideForkPointDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class BaseOverrideForkPointAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeyProject,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeySelectedBranchName {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }
  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getSelectedBranchName(anActionEvent);
    var branch = branchName.flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn));

    if (branch.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.machete-branch"), "Override Fork Point"));
    } else if (branch.get().isNonRootBranch()) {
      presentation.setDescription(
          format(getString("action.GitMachete.BaseOverrideForkPointAction.description"), branch.get().getName()));

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(getString("action.GitMachete.BaseOverrideForkPointAction.text.current-branch"));
      }
    } else {
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription(
            format(getString("action.GitMachete.BaseOverrideForkPointAction.description.root.branch"), branch.get().getName()));
      } else { //contextmenu
        // in case of root branch we do not want to show this option at all
        presentation.setEnabledAndVisible(false);
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var selectedVcsRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var branchUnderAction = getSelectedBranchName(anActionEvent);
    var branch = branchUnderAction.flatMap(pn -> getGitMacheteBranchByName(anActionEvent, pn)).getOrNull();

    if (selectedVcsRepository == null || branch == null || branch.isRootBranch()) {
      return;
    }

    var nonRootBranch = branch.asNonRootBranch();
    var selectedCommit = new OverrideForkPointDialog(project, nonRootBranch.getParentBranch(), nonRootBranch)
        .showAndGetSelectedCommit();
    if (selectedCommit == null) {
      log().debug(
          "Selected commit to be the new fork point is null: most likely the action has been canceled from override-fork-point dialog");
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

  /**
   * This method must NOT be called on UI thread.
   */
  private void overrideForkPoint(AnActionEvent anActionEvent, IGitMacheteBranch branch, IGitMacheteCommit forkPoint) {
    var selectedVcsRepository = getSelectedGitRepository(anActionEvent);

    if (selectedVcsRepository.isDefined()) {
      var root = selectedVcsRepository.get().getRoot();
      var project = getProject(anActionEvent);
      setOverrideForkPointConfigKeyValues(project, root, branch.getName(), forkPoint, branch.getPointedCommit());
    }

    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }

  /**
   * This method must NOT be called on UI thread.
   */
  private void setOverrideForkPointConfigKeyValues(
      Project project,
      VirtualFile root,
      String branchName,
      IGitMacheteCommit forkPoint,
      IGitMacheteCommit ancestorCommit) {
    var section = "machete";
    var subsectionPrefix = "overrideForkPoint";
    var to = "to";
    var whileDescendantOf = "whileDescendantOf";

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
