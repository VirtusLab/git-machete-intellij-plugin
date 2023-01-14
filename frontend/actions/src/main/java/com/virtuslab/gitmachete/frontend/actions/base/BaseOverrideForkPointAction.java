package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.backgroundables.OverrideForkPointBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.dialogs.OverrideForkPointDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.qual.async.ContinuesInBackground;

public abstract class BaseOverrideForkPointAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      ISyncToParentStatusDependentAction {

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseOverrideForkPointAction.description-action-name");
  }

  @Override
  public @Untainted @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getNonHtmlString("action.GitMachete.BaseOverrideForkPointAction.description");
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val branchUnderAction = getNameOfBranchUnderAction(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchUnderAction);

    if (gitRepository == null || branch == null || branch.isRoot()) {
      return;
    }
    val nonRootBranch = branch.asNonRoot();
    val selectedCommit = new OverrideForkPointDialog(project, nonRootBranch).showAndGetSelectedCommit();

    new OverrideForkPointBackgroundable(getString("action.GitMachete.BaseOverrideForkPointAction.task.title"),
        gitRepository, nonRootBranch,
        getGraphTable(anActionEvent),
        selectedCommit).queue();
  }

}
