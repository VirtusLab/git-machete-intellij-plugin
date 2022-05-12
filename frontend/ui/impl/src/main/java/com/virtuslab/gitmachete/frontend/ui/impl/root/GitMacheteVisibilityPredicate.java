package com.virtuslab.gitmachete.frontend.ui.impl.root;

import com.intellij.openapi.project.Project;
import com.intellij.util.NotNullFunction;
import com.intellij.vcs.log.impl.VcsProjectLog;
import lombok.CustomLog;

@CustomLog
public class GitMacheteVisibilityPredicate implements NotNullFunction<Project, Boolean> {

  @Override
  public Boolean fun(Project project) {
    boolean predicateResult = !VcsProjectLog.getLogProviders(project).isEmpty();
    LOG.debug(() -> "Visibility predicate returned ${predicateResult}");
    return predicateResult;
  }
}
