package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.defs.ActionIds.OPEN_MACHETE_FILE;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.SLIDE_IN_UNMANAGED_BELOW;
import static com.virtuslab.gitmachete.frontend.defs.PropertiesComponentKeys.SHOW_UNMANAGED_BRANCH_NOTIFICATION;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.CustomizedDataContext;
import com.intellij.openapi.actionSystem.DataSnapshotProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
@RequiredArgsConstructor
public class UnmanagedBranchNotificationFactory {
  private final Project project;
  private final @Nullable IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;
  private final GitRepository gitRepository;
  private final String branchName;
  private final @Nullable ILocalBranchReference inferredParent;

  public UnmanagedBranchNotification create() {
    val notification = new UnmanagedBranchNotification(branchName);

    val slideInAction = getSlideInAction();
    val openMacheteFileAction = getOpenMacheteFileAction();
    val dontShowForThisBranchAction = getDontShowForThisBranchAction(notification);
    val dontShowForThisProjectAction = getDontShowForThisProjectAction(notification);

    notification.addAction(slideInAction);
    notification.addAction(openMacheteFileAction);
    notification.addAction(dontShowForThisBranchAction);
    notification.addAction(dontShowForThisProjectAction);

    return notification;
  }

  @UIEffect
  public static boolean shouldShowForThisProject(Project project) {
    return PropertiesComponent.getInstance(project).getBoolean(SHOW_UNMANAGED_BRANCH_NOTIFICATION, /* defaultValue */ true);
  }

  @UIEffect
  public static boolean shouldShowForThisBranch(Project project, GitRepository gitRepository, String aBranchName) {
    return PropertiesComponent.getInstance(project)
        .getBoolean(propertyKeyForBranch(gitRepository, aBranchName), /* defaultValue */ true);
  }

  // We qualify the key with the absolute repo root path so that two repos in a multi-root project
  // (or two separately-opened projects sharing the IDE-wide PropertiesComponent) holding a branch
  // with the same name don't share the "Don't show for this branch" state.
  static String propertyKeyForBranch(GitRepository gitRepository, String aBranchName) {
    return "${SHOW_UNMANAGED_BRANCH_NOTIFICATION}.${gitRepository.getRoot().getPath()}.${aBranchName}";
  }

  private NotificationAction getSlideInAction() {
    val title = inferredParent == null
        ? getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.slide-in-as-root")
        : getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.slide-in")
            .fmt(inferredParent.getName());
    val nullableInferredParentName = inferredParent != null ? inferredParent.getName() : null;
    DataSnapshotProvider provider = sink -> {
      sink.set(DataKeys.GIT_MACHETE_REPOSITORY_SNAPSHOT, gitMacheteRepositorySnapshot);
      sink.set(DataKeys.SELECTED_BRANCH_NAME, nullableInferredParentName);
      sink.set(DataKeys.UNMANAGED_BRANCH_NAME, branchName);
      sink.set(CommonDataKeys.PROJECT, project);
    };
    return NotificationAction.create(
        title,
        (e, notif) -> {
          val dataContext = CustomizedDataContext.withSnapshot(e.getDataContext(), provider);
          val actionEvent = AnActionEvent.createEvent(dataContext, new Presentation(),
              ActionPlaces.VCS_NOTIFICATION, ActionUiKind.NONE, /* inputEvent */ null);
          ActionUtil.performAction(ActionManager.getInstance().getAction(SLIDE_IN_UNMANAGED_BELOW), actionEvent);
          notif.expire();
        });
  }

  private NotificationAction getDontShowForThisBranchAction(Notification notification) {
    return NotificationAction
        .createSimple(
            getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.dont-show-for-branch")
                .fmt(branchName),
            () -> {
              PropertiesComponent.getInstance(project)
                  .setValue(propertyKeyForBranch(gitRepository, branchName), false, /* defaultValue */ true);
              notification.expire();
            });
  }

  private NotificationAction getDontShowForThisProjectAction(Notification notification) {
    return NotificationAction
        .createSimple(
            getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.dont-show-for-project"),
            () -> {
              PropertiesComponent.getInstance(project).setValue(SHOW_UNMANAGED_BRANCH_NOTIFICATION, false,
                  /* defaultValue */ true);
              notification.expire();
            });
  }

  private NotificationAction getOpenMacheteFileAction() {
    DataSnapshotProvider provider = sink -> sink.set(CommonDataKeys.PROJECT, project);
    return NotificationAction.create(
        getString("action.GitMachete.OpenMacheteFileAction.description"),
        (e, notif) -> {
          val dataContext = CustomizedDataContext.withSnapshot(e.getDataContext(), provider);
          val actionEvent = AnActionEvent.createEvent(dataContext, new Presentation(),
              ActionPlaces.VCS_NOTIFICATION, ActionUiKind.NONE, /* inputEvent */ null);
          ActionUtil.performAction(ActionManager.getInstance().getAction(OPEN_MACHETE_FILE), actionEvent);
        });
  }
}
