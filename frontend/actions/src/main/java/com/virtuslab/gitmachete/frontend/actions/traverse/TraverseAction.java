package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Collections;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
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
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
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
            SHOW_TRAVERSE_INFO,
            /* myHeight */ 100);

        yesNoResult = traverseInfoDialog.showAndGet();
      }

      if (yesNoResult) {
        var firstEntry = branchLayout.getRootEntries().headOption().getOrNull();
        if (firstEntry != null) {
          val firstEntryName = firstEntry.getName();
          checkoutAndTraverse(gitRepository, graphTable, firstEntryName);
        }
      }
    }
  }

  private void checkoutAndTraverse(GitRepository gitRepository, BaseEnhancedGraphTable graphTable, String branchName) {
    log().debug(() -> "Queuing '${branchName}' branch checkout background task");

    @UI Runnable traverseRunnable = () -> traverse(gitRepository, graphTable, branchName);
    Runnable repositoryRefreshRunnable = () -> graphTable.queueRepositoryUpdateAndModelRefresh(traverseRunnable);
    Runnable callInAwtLater = () -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, repositoryRefreshRunnable);
    val gitBrancher = GitBrancher.getInstance(gitRepository.getProject());
    val repositories = Collections.singletonList(gitRepository);
    gitBrancher.checkout(/* reference */ branchName, /* detach */ false, repositories, callInAwtLater);
  }

  @UIEffect
  private void traverse(GitRepository gitRepository, BaseEnhancedGraphTable graphTable, String branchName) {
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;
    val gitMacheteBranch = repositorySnapshot != null ? repositorySnapshot.getManagedBranchByName(branchName) : null;

    if (gitMacheteBranch != null) {
      @UI Runnable traverseNextEntry = () -> {
        var nextBranch = branchLayout != null ? branchLayout.findNextEntry(branchName) : null;
        if (nextBranch != null) {
          checkoutAndTraverse(gitRepository, graphTable, nextBranch.getName());
        }
      };

      if (gitMacheteBranch.isNonRoot()) {
        assert repositorySnapshot != null : "repositorySnapshot is null";
        new TraverseSyncToParent(gitRepository, graphTable, repositorySnapshot, gitMacheteBranch.asNonRoot(), traverseNextEntry)
            .sync();
      } else {
        new TraverseSyncToRemote(gitRepository, graphTable, gitMacheteBranch, traverseNextEntry).sync();
      }
    }
  }
}
