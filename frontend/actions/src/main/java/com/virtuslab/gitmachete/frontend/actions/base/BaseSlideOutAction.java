package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.common.SlideOut;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public abstract class BaseSlideOutAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName != null
        ? getManagedBranchByName(anActionEvent, branchName)
        : null;

    if (branch == null) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.description.disabled.undefined.machete-branch")
          .fmt("Slide out", getQuotedStringOrCurrent(branchName)));
    } else {
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.BaseSlideOutAction.description").fmt(branch.getName()));
    }
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchName);
    if (branch != null) {
      doSlideOut(anActionEvent, branch);
    }
  }

  @ContinuesInBackground
  @UIEffect
  private void doSlideOut(AnActionEvent anActionEvent, IManagedBranchSnapshot branchToSlideOut) {
    log().debug(() -> "Entering: branchToSlideOut = ${branchToSlideOut}");
    log().debug("Refreshing repository state");

    val branchLayout = getBranchLayout(anActionEvent);
    val selectedGitRepository = getSelectedGitRepository(anActionEvent);

    if (branchLayout == null) {
      log().debug("branchLayout is null");
    } else if (selectedGitRepository == null) {
      log().debug("selectedGitRepository is null");
    } else {
      val currentMacheteBranchIfManaged = getCurrentMacheteBranchIfManaged(anActionEvent);
      val graphTable = getGraphTable(anActionEvent);
      new SlideOut(branchToSlideOut, selectedGitRepository, currentMacheteBranchIfManaged, branchLayout, graphTable).run();
    }
  }

}
