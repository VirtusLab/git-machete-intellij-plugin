package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle;
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
    if (branchLayout.isDefined()) {
      boolean anyChildBranchExists = branchLayout.get().getRootEntries()
          .exists(rootBranch -> rootBranch.getChildren().nonEmpty());

      if (anyChildBranchExists) {
        boolean anyCommitExists = getGitMacheteRepositorySnapshot(anActionEvent)
            .map(repo -> repo.getRootBranches()
                .flatMap(root -> root.getDownstreamBranches())
                .exists(b -> b.getCommits().nonEmpty()))
            .getOrElse(false);

        if (anyCommitExists) {
          presentation.setEnabled(true);
          presentation.setDescription(GitMacheteBundle.message("action.toggle-listing-commits.description"));
        } else {
          presentation.setEnabled(false);
          presentation
              .setDescription(GitMacheteBundle.message("action.toggle-listing_commits.description.disabled.no-commits"));
        }
      } else {
        presentation.setEnabled(false);
        presentation
            .setDescription(GitMacheteBundle.message("action.toggle-listing-commits.description.disabled.no-child-branches"));
      }
    } else {
      presentation.setEnabled(false);
      presentation.setDescription(GitMacheteBundle.message("action.toggle-listing-commits.description.disabled.no-branches"));
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
