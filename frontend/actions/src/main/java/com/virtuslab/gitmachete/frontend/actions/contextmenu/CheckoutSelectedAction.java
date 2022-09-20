package com.virtuslab.gitmachete.frontend.actions.contextmenu;

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
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class CheckoutSelectedAction extends BaseGitMacheteRepositoryReadyAction
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
    if (selectedBranchName == null || selectedBranchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.CheckoutSelectedAction.undefined.branch-name"));
      return;
    }

    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);

    if (currentBranchName != null && currentBranchName.equals(selectedBranchName)) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.CheckoutSelectedAction.description.disabled.currently-checked-out")
              .format(selectedBranchName));

    } else {
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.CheckoutSelectedAction.description.precise")
              .format(selectedBranchName));
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val selectedBranchName = getSelectedBranchName(anActionEvent);
    if (selectedBranchName == null || selectedBranchName.isEmpty()) {
      return;
    }

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);

    if (gitRepository != null) {
      log().debug(() -> "Queuing '${selectedBranchName}' branch checkout background task");
      new Task.Backgroundable(project, getString("action.GitMachete.CheckoutSelectedAction.task-title")) {
        @Override
        @UIThreadUnsafe
        public void run(ProgressIndicator indicator) {
          doCheckout(project, indicator, selectedBranchName, gitRepository);
        }
        // TODO (#95): on success, refresh only indication of the current branch
      }.queue();
    }
  }

  @UIThreadUnsafe
  public static void doCheckout(Project project, @NonNull ProgressIndicator indicator, String branchToCheckoutName,
      GitRepository gitRepository) {
    val uiHandler = new GitBranchUiHandlerImpl(project, indicator);
    new GitBranchWorker(project, Git.getInstance(), uiHandler)
        .checkout(branchToCheckoutName, /* detach */ false, Collections.singletonList(gitRepository));
  }
}
