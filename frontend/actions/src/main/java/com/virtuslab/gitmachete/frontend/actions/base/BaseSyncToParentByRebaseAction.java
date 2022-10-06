package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Arrays;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.CheckedFunction0;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod({Arrays.class, GitMacheteBundle.class})
@CustomLog
public abstract class BaseSyncToParentByRebaseAction extends BaseRebaseAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository {
  private static final String NL = System.lineSeparator();

  @UIEffect
  protected void setProperSuccessPresentation(AnActionEvent anActionEvent) {
    anActionEvent.getPresentation().setText(getString("action.GitMachete.BaseSyncToParentByRebaseAction.text"));
  }

  @Override
  protected boolean isRebaseTargetPresent(AnActionEvent anActionEvent) {
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchName);
    return branch != null && branch.isNonRoot();
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    val presentation = anActionEvent.getPresentation();

    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchName);
    if (branch != null) {
      if (branch.isRoot()) {
        if (anActionEvent.getPlace().equals(ActionPlaces.TOOLBAR)) {
          presentation.setEnabled(false);
          presentation.setDescription(
              getNonHtmlString("action.GitMachete.BaseSyncToParentByRebaseAction.description.disabled.root-branch")
                  .format(branch.getName()));
        } else { //contextmenu
          // in case of root branch we do not want to show this option at all
          presentation.setEnabledAndVisible(false);
        }

      } else if (branch.isNonRoot()) {
        val nonRootBranch = branch.asNonRoot();
        val upstream = nonRootBranch.getParent();
        presentation.setDescription(getNonHtmlString("action.GitMachete.BaseRebaseAction.description")
            .format(branch.getName(), upstream.getName()));
      }
    }
  }

  protected CheckedFunction0<IGitRebaseParameters> getParametersForRebase(IManagedBranchSnapshot branchToRebase) {
    return () -> {
      if (branchToRebase.isNonRoot()) {
        return branchToRebase.asNonRoot().getParametersForRebaseOntoParent();
      } else {
        throw new GitMacheteMissingForkPointException();
      }
    };
  }
}
