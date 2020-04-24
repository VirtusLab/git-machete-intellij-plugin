package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.keys.ActionIDs.ACTION_REFRESH;

import java.util.Set;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
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
  private static final Logger LOG = Logger.getInstance(BaseSlideOutBranchAction.class);
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

    Project project = anActionEvent.getProject();
    assert project != null;

    var branchLayout = anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT);
    assert branchLayout != null;

    var branchName = branchToSlideOut.getName();

    try {
      var newBranchLayout = branchLayout.slideOut(branchName);
      var macheteFilePath = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_FILE_PATH);
      var branchLayoutFileSaver = branchLayoutSaverFactory.create(macheteFilePath);

      branchLayoutFileSaver.save(newBranchLayout, /* backupOldLayout */ true);
      ActionManager.getInstance().getAction(ACTION_REFRESH).actionPerformed(anActionEvent);
      VcsNotifier.getInstance(project).notifySuccess("Branch <b>${branchName}</b> slid out");
    } catch (BranchLayoutException e) {
      String message = e.getMessage();
      VcsNotifier.getInstance(project).notifyError("Slide out of <b>${branchName}</b> failed",
          message == null ? "" : message);
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
