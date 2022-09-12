package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;

@CustomLog
public class ToggleListingCommitsAction extends BaseGitMacheteRepositoryReadyAction
    implements
      DumbAware,
      IExpectsKeyGitMacheteRepository {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    final var presentation = anActionEvent.getPresentation();
    final var selected = Toggleable.isSelected(presentation);
    Toggleable.setSelected(presentation, selected);
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    final var branchLayout = getBranchLayout(anActionEvent);
    if (branchLayout == null) {
      presentation.setEnabled(false);
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.ToggleListingCommitsAction.description.disabled.no-branches"));
      return;
    }

    final var gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent);

    final var managedBranches = gitMacheteRepositorySnapshot != null
        ? gitMacheteRepositorySnapshot.getManagedBranches()
        : null;

    final var anyCommitExists = managedBranches != null &&
        managedBranches.exists(b -> b.isNonRoot() && b.asNonRoot().getCommits().nonEmpty());

    if (anyCommitExists) {
      presentation.setEnabled(true);
      presentation.setDescription(getNonHtmlString("action.GitMachete.ToggleListingCommitsAction.description"));
    } else {
      presentation.setEnabled(false);
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.ToggleListingCommitsAction.description.disabled.no-commits"));
    }

  }

  @Override
  @UIEffect
  public final void actionPerformed(AnActionEvent anActionEvent) {
    boolean newState = !Toggleable.isSelected(anActionEvent.getPresentation());
    log().debug("Triggered with newState = ${newState}");

    final var graphTable = getGraphTable(anActionEvent);
    graphTable.setListingCommits(newState);
    graphTable.refreshModel();

    final var presentation = anActionEvent.getPresentation();
    Toggleable.setSelected(presentation, newState);
  }
}
