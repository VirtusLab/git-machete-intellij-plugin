package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseResetAction;
import com.virtuslab.gitmachete.frontend.actions.base.IBranchNameProvider;
import com.virtuslab.gitmachete.frontend.actions.base.ISyncToParentStatusDependentAction;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public class ResetCurrentToParentAction extends BaseResetAction
    implements
      IBranchNameProvider,
      ISyncToParentStatusDependentAction {

  protected String getResetOptionsString() {
    return "";
  }

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.ResetCurrentToParentAction.description-action-name");
  }

  @Override
  public @Untainted @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getNonHtmlString("action.GitMachete.ResetCurrentToParentAction.description.enabled");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(
        SyncToParentStatus.OutOfSync);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
  }

  protected @Nullable IBranchReference getTargetBranch(AnActionEvent anActionEvent) {
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val localBranch = branchName != null ? getManagedBranchByName(anActionEvent, branchName) : null;
    return localBranch != null ? localBranch.getRemoteTrackingBranch() : null;
  }

  protected String getTargetBranchName(IBranchReference targetBranch) {
    return targetBranch.getName();
  }

  protected String getRelationToTargetBranch() {
    return "remote";
  }

  @Override
  protected void handleResetNonCurrentToTarget(Project project,
      GitRepository gitRepository,
      ILocalBranchReference localBranch,
      IBranchReference targetBranchReference) {
    throw new io.vavr.NotImplementedError();
  }

  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }
}
