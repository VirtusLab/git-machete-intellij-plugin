package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class ToggleListingCommitsAction extends BaseGitMacheteRepositoryReadyAction
    implements
      DumbAware,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    boolean selected = isSelected(presentation);
    Toggleable.setSelected(presentation, selected);
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchLayout = getBranchLayout(anActionEvent);
    if (branchLayout.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getString("action.GitMachete.ToggleListingCommitsAction.description.disabled.no-branches"));
      return;
    }

    boolean noChildBranchExists = branchLayout.get().getRootEntries()
        .exists(rootBranch -> rootBranch.getChildren().isEmpty());

    if (noChildBranchExists) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getString("action.GitMachete.ToggleListingCommitsAction.description.disabled.no-child-branches"));
      return;
    }

    boolean anyCommitExists = getGitMacheteRepositorySnapshot(anActionEvent)
        .map(repo -> repo.getRootBranches()
            .flatMap(root -> root.getChildBranches())
            .exists(b -> b.getCommits().nonEmpty()))
        .getOrElse(false);

    if (anyCommitExists) {
      presentation.setEnabled(true);
      presentation.setDescription(getString("action.GitMachete.ToggleListingCommitsAction.description"));
    } else {
      presentation.setEnabled(false);
      presentation.setDescription(getString("action.GitMachete.ToggleListingCommitsAction.description.disabled.no-commits"));
    }

  }

  @Override
  @UIEffect
  public final void actionPerformed(AnActionEvent e) {
    boolean state = !isSelected(e);
    setSelected(e, state);
    var presentation = e.getPresentation();
    Toggleable.setSelected(presentation, state);
  }

  @UIEffect
  private boolean isSelected(AnActionEvent anActionEvent) {
    var presentation = anActionEvent.getPresentation();
    return Toggleable.isSelected(presentation);
  }

  @UIEffect
  private boolean isSelected(Presentation presentation) {
    return Toggleable.isSelected(presentation);
  }

  @UIEffect
  public void setSelected(AnActionEvent anActionEvent, boolean state) {
    log().debug("Triggered with state = ${state}");

    var graphTable = getGraphTable(anActionEvent);
    graphTable.setListingCommits(state);
    graphTable.refreshModel();
  }
}
