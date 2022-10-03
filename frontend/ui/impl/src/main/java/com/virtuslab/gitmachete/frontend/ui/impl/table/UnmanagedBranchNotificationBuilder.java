package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.SLIDE_IN_UNMANAGED_BELOW;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;

@RequiredArgsConstructor
public class UnmanagedBranchNotificationBuilder {

  private final Project project;
  private final @Nullable IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;
  private final String branchName;
  private final String inferredParent;

  public Notification build() {
    val notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
        getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.text").format(branchName),
        NotificationType.INFORMATION);

    val slideInAction = getSlideInAction(branchName, inferredParent, notification);
    val dontShowForThisBranchAction = getDontShowForThisBranchAction(branchName, notification);
    val dontShowForThisProjectAction = getDontShowForThisProjectAction(notification);

    notification.addAction(slideInAction);
    notification.addAction(dontShowForThisBranchAction);
    notification.addAction(dontShowForThisProjectAction);

    return notification;
  }

  private NotificationAction getSlideInAction(String branchName, String inferredParent, Notification notification) {
    return NotificationAction
        .createSimple(
            getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.slide-in")
                .format(inferredParent),
            () -> {
              val dataContext = new DataContext() {
                @Override
                public @Nullable Object getData(String dataId) {
                  return Match(dataId).of(
                      typeSafeCase(DataKeys.GIT_MACHETE_REPOSITORY_SNAPSHOT, gitMacheteRepositorySnapshot),
                      typeSafeCase(DataKeys.SELECTED_BRANCH_NAME, inferredParent),
                      typeSafeCase(DataKeys.UNMANAGED_BRANCH_NAME, branchName),
                      typeSafeCase(CommonDataKeys.PROJECT, project),
                      Case($(), (Object) null));
                }
              };
              val actionEvent = AnActionEvent.createFromDataContext(ActionPlaces.VCS_NOTIFICATION, new Presentation(),
                  dataContext);
              ActionManager.getInstance().getAction(SLIDE_IN_UNMANAGED_BELOW).actionPerformed(actionEvent);
              notification.expire();
            });
  }

  private NotificationAction getDontShowForThisBranchAction(String branchName, Notification notification) {
    return NotificationAction
        .createSimple(
            getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.dont-show-for-branch")
                .format(branchName),
            () -> {
              notification.expire();
            });

  }

  private NotificationAction getDontShowForThisProjectAction(Notification notification) {
    return NotificationAction
        .createSimple(
            getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.dont-show-for-project"),
            () -> {
              notification.expire();
            });

  }
}
