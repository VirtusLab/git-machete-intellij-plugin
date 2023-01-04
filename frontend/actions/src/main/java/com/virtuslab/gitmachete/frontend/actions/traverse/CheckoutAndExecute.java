package com.virtuslab.gitmachete.frontend.actions.traverse;

import java.util.Collections;

import com.intellij.openapi.application.ModalityState;
import com.intellij.util.ModalityUiUtil;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;

import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.qual.async.ContinuesInBackground;

@CustomLog
final class CheckoutAndExecute {
  private CheckoutAndExecute() {}

  @ContinuesInBackground
  static void checkoutAndExecuteOnUIThread(GitRepository gitRepository, BaseEnhancedGraphTable graphTable, String branchName,
      @UI Runnable doOnUIThreadAfterCheckout) {
    val currentBranch = gitRepository.getCurrentBranch();
    if (currentBranch != null && currentBranch.getName().equals(branchName)) {
      ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, doOnUIThreadAfterCheckout);
    } else {
      LOG.debug(() -> "Queuing '${branchName}' branch checkout background task");

      Runnable repositoryRefreshRunnable = () -> graphTable.queueRepositoryUpdateAndModelRefresh(doOnUIThreadAfterCheckout);
      val gitBrancher = GitBrancher.getInstance(gitRepository.getProject());
      val repositories = Collections.singletonList(gitRepository);

      gitBrancher.checkout(/* reference */ branchName, /* detach */ false, repositories, repositoryRefreshRunnable);
    }
  }

}
