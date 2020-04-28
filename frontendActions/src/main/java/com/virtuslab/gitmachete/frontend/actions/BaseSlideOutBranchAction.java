package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.keys.ActionIDs.ACTION_REFRESH;

import java.util.Set;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;
import lombok.SneakyThrows;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.reflections.Reflections;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayoutSaverFactory;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.frontend.keys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_FILE_PATH}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BaseSlideOutBranchAction extends GitMacheteRepositoryReadyAction {
  public static final LambdaLogger LOG = LambdaLoggerFactory.getLogger("frontendActions");

  private final IBranchLayoutSaverFactory branchLayoutSaverFactory = getBranchLayoutSaverFactoryInstance();

  public BaseSlideOutBranchAction(String text, String actionDescription, Icon icon) {
    super(text, actionDescription, icon);
  }

  public BaseSlideOutBranchAction() {}

  /**
   * Bear in mind that {@link AnAction#beforeActionPerformedUpdate} is called before each action.
   * (For more details check {@link com.intellij.openapi.actionSystem.ex.ActionUtil} as well.)
   * The {@link AnActionEvent} argument passed to before-called {@link AnAction#update} is the same one that is passed here.
   * This gives us certainty that all checks from actions' update implementations will be performed
   * and all data available via data keys in those {@code update} implementations will still do be available
   * in {@link BaseSlideOutBranchAction#actionPerformed} implementations.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);

  @UIEffect
  public void doSlideOut(AnActionEvent anActionEvent, BaseGitMacheteNonRootBranch branchToSlideOut) {
    LOG.debug(() -> "Entering: anActionEvent = ${anActionEvent}, " +
        "branchToSlideOut = ${branchToSlideOut} (${branchToSlideOut.getName()})");
    Project project = anActionEvent.getProject();
    assert project != null : "Can't get project from anActionEvent variable";

    var branchLayout = anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT);
    assert branchLayout != null : "Can't get branch layout";

    var branchName = branchToSlideOut.getName();

    try {
      LOG.info(() -> "Sliding out '${branchName}' branch in memory");
      var newBranchLayout = branchLayout.slideOut(branchName);
      var macheteFilePath = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_FILE_PATH);
      var branchLayoutFileSaver = branchLayoutSaverFactory.create(macheteFilePath);

      LOG.info("Saving new branch layout into file");
      branchLayoutFileSaver.save(newBranchLayout, /* backupOldLayout */ true);

      LOG.debug("Refreshing repository state");
      ActionManager.getInstance().getAction(ACTION_REFRESH).actionPerformed(anActionEvent);
      VcsNotifier.getInstance(project).notifySuccess("Branch <b>${branchName}</b> slid out");
    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out '${branchName}' branch" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError("Slide out of <b>${branchName}</b> failed",
          exceptionMessage == null ? "" : exceptionMessage);
      GuiUtils.invokeLaterIfNeeded(() -> Messages.showErrorDialog(errorMessage, "Something Went Wrong..."),
          ModalityState.NON_MODAL);
    }
  }

  @SneakyThrows
  private static IBranchLayoutSaverFactory getBranchLayoutSaverFactoryInstance() {
    Reflections reflections = new Reflections("com.virtuslab");
    Set<Class<? extends IBranchLayoutSaverFactory>> classes = reflections
        .getSubTypesOf(IBranchLayoutSaverFactory.class);
    return classes.iterator().next().getDeclaredConstructor().newInstance();
  }
}
