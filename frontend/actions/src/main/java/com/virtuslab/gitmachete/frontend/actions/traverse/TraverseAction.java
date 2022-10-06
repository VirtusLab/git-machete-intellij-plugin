package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.actions.traverse.TraverseSyncToParent.syncBranchToParent;
import static com.virtuslab.gitmachete.frontend.actions.traverse.TraverseSyncToRemote.syncBranchToRemote;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Collections;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.ModalityUiUtil;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.InfoDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.defs.ActionIds;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
@CustomLog
public class TraverseAction extends BaseGitMacheteRepositoryReadyAction implements IExpectsKeySelectedBranchName {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  public static final String SHOW_TRAVERSE_INFO = "git-machete.traverse.approval.show";

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }
    presentation.setDescription(getNonHtmlString("action.GitMachete.TraverseAction.description.text"));

    val graphTable = getGraphTable(anActionEvent);
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;

    if (branchLayout == null || branchLayout.getRootEntries().isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.TraverseAction.description.empty-layout"));
      return;
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val graphTable = getGraphTable(anActionEvent);
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;
    val project = getProject(anActionEvent);

    boolean yesNoResult = true;
    if (branchLayout != null && branchLayout.getRootEntries().nonEmpty() && gitRepository != null) {
      if (PropertiesComponent.getInstance(project).getBoolean(SHOW_TRAVERSE_INFO, /* defaultValue */ true)) {

        val traverseInfoDialog = new InfoDialog(
            project,
            getString("action.GitMachete.TraverseAction.dialog.traverse-approval.title"),
            getString("action.GitMachete.TraverseAction.dialog.traverse-approval.text.HTML"),
            SHOW_TRAVERSE_INFO);

        yesNoResult = traverseInfoDialog.showAndGet();
      }

      if (yesNoResult) {
        var firstEntry = branchLayout.getRootEntries().headOption().getOrNull();
        if (firstEntry != null) {
          val firstEntryName = firstEntry.getName();
          checkoutAndTraverse(anActionEvent, firstEntryName, project, gitRepository);
        }
      }
    }
  }

  private void checkoutAndTraverse(AnActionEvent anActionEvent, String branchName, Project project,
      GitRepository gitRepository) {
    log().debug(() -> "Queuing '${branchName}' branch checkout background task");

    Runnable callInAwtLater = () -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL,
        () -> traverse(branchName, anActionEvent, gitRepository));

    GitBrancher.getInstance(project).checkout(/* reference */ branchName, /* detach */ false,
        Collections.singletonList(gitRepository), callInAwtLater);
  }

  @UIEffect
  private void traverse(String branchName, AnActionEvent anActionEvent, GitRepository gitRepository) {
    val project = getProject(anActionEvent);
    val graphTable = getGraphTable(anActionEvent);
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;
    val gitMacheteBranch = repositorySnapshot != null ? repositorySnapshot.getManagedBranchByName(branchName) : null;

    if (gitMacheteBranch != null) {
      @UI Runnable traverseNextEntry = () -> {
        var nextBranch = branchLayout != null ? branchLayout.findNextEntry(branchName) : null;
        if (nextBranch != null) {

          val checkoutNextAction = ActionManager.getInstance().getAction(ActionIds.CHECK_OUT_NEXT);
          checkoutNextAction.actionPerformed(anActionEvent);
          traverse(nextBranch.getName(), anActionEvent, gitRepository);
        }
      };

      if (gitMacheteBranch.isNonRoot()) {
        syncBranchToParent(project, graphTable, gitMacheteBranch.asNonRoot(), gitRepository, traverseNextEntry);
      } else {
        syncBranchToRemote(project, graphTable, gitMacheteBranch.getRemoteTrackingBranch(), branchName, gitRepository,
            traverseNextEntry);
      }
    }
  }
}
