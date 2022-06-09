package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.impl.VcsLogContentUtil;
import com.intellij.vcs.log.impl.VcsProjectLog;
import git4idea.branch.GitBranchesCollection;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class ShowSelectedBranchInGitLogAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeySelectedBranchName {

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
    if (selectedBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.ShowSelectedBranchInGitLogAction.undefined.branch-name"));
      return;
    }

    presentation.setDescription(
        getNonHtmlString("action.GitMachete.ShowSelectedBranchInGitLogAction.description.precise")
            .format(selectedBranchName.get()));
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val selectedBranchName = getSelectedBranchName(anActionEvent);
    if (selectedBranchName.isEmpty()) {
      return;
    }

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository.isDefined()) {
      log().debug(() -> "Queuing show '${selectedBranchName.get()}' branch in Git log background task");

      GitBranchesCollection branches = gitRepository.get().getBranches();
      @SuppressWarnings("nullness:return") val maybeHash = selectedBranchName
          .map(branches::findBranchByName).filter(Objects::nonNull).map(branches::getHash);
      if (maybeHash.isEmpty()) {
        log().error("Unable to find commit hash for branch '${selectedBranchName.get()}'");
        return;
      }

      VcsLogContentUtil.runInMainLog(project, logUi -> jumpToRevisionUnderProgress(project, maybeHash.get()));
    }
  }

  private void jumpToRevisionUnderProgress(Project project, Hash hash) {
    new Task.Backgroundable(project, getString("action.GitMachete.ShowSelectedBranchInGitLogAction.task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        try {
          val logUi = VcsProjectLog.getInstance(project).getMainLogUi();
          if (logUi == null) {
            log().error("Main log ui is null");
            return;
          }
          logUi.getVcsLog().jumpToReference(hash.asString()).get();
        } catch (CancellationException | InterruptedException ignored) {} catch (ExecutionException e) {
          String msg = "Error while showing branch in Git log";
          if (e.getMessage() != null) {
            msg = e.getMessage();
          }
          log().error(msg);
        }
      }
    }.queue();
  }

}
