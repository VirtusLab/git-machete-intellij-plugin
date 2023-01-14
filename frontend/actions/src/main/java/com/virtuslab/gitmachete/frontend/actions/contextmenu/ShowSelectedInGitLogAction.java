package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.impl.VcsLogContentUtil;
import com.intellij.vcs.log.impl.VcsProjectLog;
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

      VcsLogContentUtil.runInMainLog(project, logUi -> jumpToRevisionUnderProgress(project, selectedBranchHash));
    }
  }

  @ContinuesInBackground
  private void jumpToRevisionUnderProgress(Project project, Hash hash) {
    val logUi = VcsProjectLog.getInstance(project).getMainLogUi();
    if (logUi == null) {
      LOG.error("Main VCS Log UI is null");
      return;
    }
    logUi.getVcsLog().jumpToReference(hash.asString());
  }

}
