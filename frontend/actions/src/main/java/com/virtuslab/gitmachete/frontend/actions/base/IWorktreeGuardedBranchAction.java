package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

/**
 * Mixin for actions whose underlying git operation either moves the branch ref (e.g. pull, reset,
 * rebase-onto-parent, fast-forward merge) or first checks the branch out (e.g. checkout, squash,
 * merge-from-parent into a non-current child). Git refuses any such operation when the target
 * branch is already checked out in another worktree, so these actions need an up-front guard that
 * disables them with a uniform "branch held by another worktree" tooltip rather than letting the
 * user discover the failure through a side-effecting error.
 *
 * <p>Mirrors {@link ISyncToParentStatusDependentAction} / {@link ISyncToRemoteStatusDependentAction}
 * in shape: a default helper that mutates the {@link AnActionEvent}'s presentation in-place. Unlike
 * those two, the branch name is passed explicitly (not pulled via {@link IBranchNameProvider}),
 * because {@code BaseCheckoutAction} sources it from a different data key.
 */
@ExtensionMethod(GitMacheteBundle.class)
public interface IWorktreeGuardedBranchAction extends IExpectsKeyGitMacheteRepository {

  /**
   * @return the absolute root of an <i>other</i> worktree currently holding {@code branchName} checked out, or
   *         {@code null} if no other worktree holds it (or the branch is not managed). "Other" means a
   *         worktree different from the one the enclosing snapshot was built against. Both paths are already
   *         canonicalized at snapshot creation time, so a plain {@link Path#equals} comparison is sufficient.
   */
  default @Nullable Path getWorktreeRootHoldingBranchIfHeldElsewhere(
      AnActionEvent anActionEvent, @Nullable String branchName) {
    val snapshot = getGitMacheteRepositorySnapshot(anActionEvent);
    val branch = getManagedBranchByName(anActionEvent, branchName);
    if (snapshot == null || branch == null) {
      return null;
    }
    val holder = branch.getWorktreeRootHoldingBranch();
    if (holder == null || holder.equals(snapshot.getRootDirectoryPath())) {
      return null;
    }
    return holder;
  }

  /**
   * Disables the action's presentation with the standard "branch held by another worktree" tooltip
   * iff {@code branchName} is held by some other worktree. Otherwise leaves the presentation
   * untouched.
   *
   * @return {@code true} iff the presentation was disabled, so callers in an if-else chain can
   *         short-circuit cleanly (mirrors the {@code BaseSquashAction} usage shape).
   */
  @UIEffect
  default boolean disableIfBranchHeldByOtherWorktree(AnActionEvent anActionEvent, @Nullable String branchName) {
    if (branchName == null) {
      return false;
    }
    val holder = getWorktreeRootHoldingBranchIfHeldElsewhere(anActionEvent, branchName);
    if (holder == null) {
      return false;
    }
    val presentation = anActionEvent.getPresentation();
    presentation.setEnabled(false);
    presentation.setDescription(
        getNonHtmlString("action.GitMachete.description.disabled.branch-held-by-other-worktree")
            .fmt(branchName, holder.toString()));
    return true;
  }
}
