package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.impl.VcsProjectLog;
import com.intellij.vcs.log.ui.VcsLogUiEx;
import kotlin.Unit;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class ShowSelectedInGitLogAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeySelectedBranchName {

  @Override
  protected boolean isSideEffecting() {
    return false;
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val selectedBranchName = getSelectedBranchName(anActionEvent);
    // It's very unlikely that selectedBranchName is empty at this point since it's assigned directly before invoking this
    // action in GitMacheteGraphTable.GitMacheteGraphTableMouseAdapter.mouseClicked; still, it's better to be safe.
    if (selectedBranchName == null || selectedBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.ShowSelectedInGitLogAction.undefined.branch-name"));
      return;
    }

    presentation.setDescription(
        getNonHtmlString("action.GitMachete.ShowSelectedInGitLogAction.description.precise").fmt(selectedBranchName));
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val selectedBranchName = getSelectedBranchName(anActionEvent);
    if (selectedBranchName == null || selectedBranchName.isEmpty()) {
      return;
    }

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository != null) {
      LOG.debug(() -> "Queuing show '${selectedBranchName}' branch in Git log background task");

      val branches = gitRepository.getBranches();

      val selectedBranch = branches.findBranchByName(selectedBranchName);
      val selectedBranchHash = selectedBranch != null ? branches.getHash(selectedBranch) : null;

      if (selectedBranchHash == null) {
        LOG.error("Unable to find commit hash for branch '${selectedBranchName}'");
        return;
      }
      VirtualFile root = gitRepository.getRoot();
      VcsProjectLog.runInMainLog(project, logUi -> jumpToRevisionUnderProgress(logUi, root, selectedBranchHash));
    }
  }

  @ContinuesInBackground
  private Unit jumpToRevisionUnderProgress(VcsLogUiEx logUi, VirtualFile root, Hash hash) {
    if (logUi == null) {
      LOG.error("Main VCS Log UI is null");
      return Unit.INSTANCE;
    }
    logUi.getVcsLog().jumpToCommit(hash, root);
    return Unit.INSTANCE;
  }

}
