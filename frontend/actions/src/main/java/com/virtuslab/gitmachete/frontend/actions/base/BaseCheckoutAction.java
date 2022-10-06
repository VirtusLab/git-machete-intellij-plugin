package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public abstract class BaseCheckoutAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IExpectsKeySelectedBranchName {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  protected abstract @Nullable String getTargetBranchName(AnActionEvent anActionEvent);

  protected abstract @Untainted String getNonExistentBranchMessage(AnActionEvent anActionEvent);

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val targetBranchName = getTargetBranchName(anActionEvent);

    // It's very unlikely that targetBranchName is empty at this point since it's assigned directly before invoking this
    // action in EnhancedGraphTable.EnhancedGraphTableMouseAdapter#mouseClicked; still, it's better to be safe.
    if (targetBranchName == null || targetBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonExistentBranchMessage(anActionEvent));
      return;
    }

    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);

    if (currentBranchName != null && currentBranchName.equals(targetBranchName)) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseCheckoutAction.description.disabled.currently-checked-out")
              .format(targetBranchName));

    } else {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseCheckoutAction.description.precise")
              .format(targetBranchName));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val targetBranchName = getTargetBranchName(anActionEvent);
    if (targetBranchName == null || targetBranchName.isEmpty()) {
      return;
    }

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository != null) {

      log().debug(() -> "Queuing '${targetBranchName}' branch checkout background task");
      new Task.Backgroundable(project, getString("action.GitMachete.BaseCheckoutAction.task-title")) {
        @Override
        @UIThreadUnsafe
        public void run(ProgressIndicator indicator) {
          doCheckout(project, indicator, targetBranchName, gitRepository);
        }
      }.queue();
    }
  }

  @UIThreadUnsafe
  public static void doCheckout(Project project, ProgressIndicator indicator, String branchToCheckoutName,
      GitRepository gitRepository) {
    val uiHandler = new GitBranchUiHandlerImpl(project, indicator);
    new GitBranchWorker(project, Git.getInstance(), uiHandler)
        .checkout(branchToCheckoutName, /* detach */ false, Collections.singletonList(gitRepository));
  }
}
