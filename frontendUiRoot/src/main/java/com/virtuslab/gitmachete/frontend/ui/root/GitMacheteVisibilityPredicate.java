package com.virtuslab.gitmachete.frontend.ui.root;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.util.NotNullFunction;
import git4idea.GitVcs;

import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public class GitMacheteVisibilityPredicate implements NotNullFunction<Project, Boolean> {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiRoot");

  @Override
  public Boolean fun(Project project) {
    // `com.intellij.openapi.vcs.ProjectLevelVcsManager#checkVcsIsActive(String)` returns `true` when the specified
    // VCS is used by at least one module in the project. Therefore it is guaranteed that Git Machete plugin tab
    // is created only when a git repository exists.
    boolean predicateResult = ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(GitVcs.NAME);
    LOG.debug(() -> "Visibility predicate returned ${predicateResult}");
    return predicateResult;
  }
}
