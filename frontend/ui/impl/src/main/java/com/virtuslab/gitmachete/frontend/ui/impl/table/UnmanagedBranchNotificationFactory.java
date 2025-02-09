package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.OPEN_MACHETE_FILE;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.SLIDE_IN_UNMANAGED_BELOW;
import static com.virtuslab.gitmachete.frontend.defs.PropertiesComponentKeys.SHOW_UNMANAGED_BRANCH_NOTIFICATION;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.CustomizedDataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
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
  private final String branchName;
  private final @Nullable ILocalBranchReference inferredParent;

  public UnmanagedBranchNotification create() {
    val notification = new UnmanagedBranchNotification(branchName);

    val slideInAction = getSlideInAction(notification);
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
  public static boolean shouldShowForThisBranch(Project project, String aBranchName) {
    String propertyKey = "${SHOW_UNMANAGED_BRANCH_NOTIFICATION}.${aBranchName}";
    return PropertiesComponent.getInstance(project).getBoolean(propertyKey, /* defaultValue */ true);
  }

  @SuppressWarnings("removal")
  private NotificationAction getSlideInAction(Notification notification) {
    val title = inferredParent == null
        ? getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.slide-in-as-root")
        : getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.slide-in")
            .fmt(inferredParent.getName());
    val nullableInferredParentName = inferredParent != null ? inferredParent.getName() : null;
    val provider = new DataProvider() {
      @Override
      public @Nullable Object getData(String dataId) {
        return Match(dataId).of(
            typeSafeCase(DataKeys.GIT_MACHETE_REPOSITORY_SNAPSHOT, gitMacheteRepositorySnapshot),
            typeSafeCase(DataKeys.SELECTED_BRANCH_NAME, nullableInferredParentName),
            typeSafeCase(DataKeys.UNMANAGED_BRANCH_NAME, branchName),
            typeSafeCase(CommonDataKeys.PROJECT, project),
            Case($(), (Object) null));
      }
    };
    return NotificationAction
        .createSimple(
            title,
            () -> {
              val dataContext = CustomizedDataContext.withProvider(DataManager.getInstance().getDataContext(), provider);
              @SuppressWarnings("removal") val actionEvent = AnActionEvent.createFromDataContext(ActionPlaces.VCS_NOTIFICATION,
                  new Presentation(), dataContext);
              ActionManager.getInstance().getAction(SLIDE_IN_UNMANAGED_BELOW).actionPerformed(actionEvent);
              notification.expire();
            });
  }

  private NotificationAction getDontShowForThisBranchAction(Notification notification) {
    return NotificationAction
        .createSimple(
            getString("action.GitMachete.EnhancedGraphTable.unmanaged-branch-notification.action.dont-show-for-branch")
                .fmt(branchName),
            () -> {
              String propertyKey = "${SHOW_UNMANAGED_BRANCH_NOTIFICATION}.${branchName}";
              PropertiesComponent.getInstance(project).setValue(propertyKey, false, /* defaultValue */ true);
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

  @SuppressWarnings("removal")
  private NotificationAction getOpenMacheteFileAction() {
    val provider = new DataProvider() {
      @Override
      public @Nullable Object getData(String dataId) {
        return dataId.equals(CommonDataKeys.PROJECT.getName()) ? project : null;
      }
    };
    return NotificationAction.createSimple(
        getString("action.GitMachete.OpenMacheteFileAction.description"), () -> {
          val dataContext = CustomizedDataContext.withProvider(DataManager.getInstance().getDataContext(), provider);
          @SuppressWarnings("removal") val actionEvent = AnActionEvent.createFromDataContext(ActionPlaces.VCS_NOTIFICATION,
              new Presentation(), dataContext);
          ActionManager.getInstance().getAction(OPEN_MACHETE_FILE).actionPerformed(actionEvent);
        });
  }
}
