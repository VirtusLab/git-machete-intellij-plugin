package com.virtuslab.gitmachete.frontend.ui.impl.root;

import java.util.function.Predicate;

import com.intellij.openapi.project.Project;
import com.intellij.util.NotNullFunction;
import com.intellij.vcs.log.impl.VcsProjectLog;
import lombok.CustomLog;

@CustomLog
public class GitMacheteVisibilityPredicate implements NotNullFunction<Project, Boolean>, Predicate<Project> {

  @Override
  public Boolean fun(Project project) {
    boolean predicateResult = !VcsProjectLog.getLogProviders(project).isEmpty();
    LOG.debug(() -> "Visibility predicate returned ${predicateResult}");
    return predicateResult;
  }

  @Override
  public boolean test(Project project) {
    return fun(project);
  }
}
