package com.virtuslab.gitmachete.frontend.ui.impl.root;

import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.BorderLayout;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.ScrollPaneFactory;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.MinLen;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.actionids.ActionGroupIds;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManagerFactory;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public final class GitMachetePanel extends SimpleToolWindowPanel implements DataProvider {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiRoot");

  public static final String GIT_MACHETE_TOOLBAR = "GitMacheteToolbar";

  private final Project project;
  private final VcsRootComboBox vcsRootComboBox;
  private final IGraphTableManager gitMacheteGraphTableManager;

  @UIEffect
  public GitMachetePanel(Project project) {
    super(/* vertical */ false, /* borderless */ true);
    LOG.debug("Instantiating");

    this.project = project;
    // GitUtil.getRepositories(project) should never return empty list because it means there's no git repository in an opened
    // project, so Git Machete plugin shouldn't even be loaded in the first place (as ensured by GitMacheteVisibilityPredicate)
    @SuppressWarnings("value:assignment.type.incompatible")
    @MinLen(1)
    List<GitRepository> repositories = List.ofAll(GitUtil.getRepositories(project));
    this.vcsRootComboBox = new VcsRootComboBox(repositories);
    this.gitMacheteGraphTableManager = RuntimeBinding
        .instantiateSoleImplementingClass(IGraphTableManagerFactory.class).create(project, vcsRootComboBox);
    gitMacheteGraphTableManager.updateAndRefreshGraphTableInBackground();

    // This class is final, so the instance is `@Initialized` at this point.

    setToolbar(createGitMacheteVerticalToolbar().getComponent());
    add(VcsRootComboBox.createShrinkingWrapper(vcsRootComboBox), BorderLayout.NORTH);
    var gitMacheteGraphTable = gitMacheteGraphTableManager.getGraphTable();
    setContent(ScrollPaneFactory.createScrollPane(gitMacheteGraphTable));
  }

  @Override
  @Nullable
  public Object getData(String dataId) {
    return Match(dataId).of(
        typeSafeCase(DataKeys.KEY_GRAPH_TABLE_MANAGER, gitMacheteGraphTableManager),
        typeSafeCase(DataKeys.KEY_SELECTED_VCS_REPOSITORY, vcsRootComboBox.getSelectedRepository().getOrNull()),
        typeSafeCase(CommonDataKeys.PROJECT, project),
        Case($(), (Object) null));
  }

  @UIEffect
  private ActionToolbar createGitMacheteVerticalToolbar() {
    var actionManager = ActionManager.getInstance();
    var toolbarActionGroup = (ActionGroup) actionManager.getAction(ActionGroupIds.ACTION_GROUP_TOOLBAR);
    var toolbar = actionManager.createActionToolbar(GIT_MACHETE_TOOLBAR, toolbarActionGroup, /* horizontal */ false);
    toolbar.setTargetComponent(gitMacheteGraphTableManager.getGraphTable());
    return toolbar;
  }
}
