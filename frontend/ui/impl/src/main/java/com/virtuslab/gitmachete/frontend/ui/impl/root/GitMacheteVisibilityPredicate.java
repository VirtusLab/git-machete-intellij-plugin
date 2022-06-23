package com.virtuslab.gitmachete.frontend.ui.impl.root;

import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.impl.VcsProjectLog;
import java.util.function.Predicate;
import lombok.CustomLog;

@CustomLog
public class GitMacheteVisibilityPredicate implements Predicate<Project> {

  @Override
  public boolean test(Project project) {
    boolean predicateResult = !VcsProjectLog.getLogProviders(project).isEmpty();
    LOG.debug(() -> "Visibility predicate returned ${predicateResult}");
    return predicateResult;
  }
}
