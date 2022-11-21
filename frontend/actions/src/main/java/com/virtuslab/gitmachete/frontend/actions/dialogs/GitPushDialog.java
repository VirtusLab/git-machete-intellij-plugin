package com.virtuslab.gitmachete.frontend.actions.dialogs;
import java.util.Collection;

import com.intellij.dvcs.push.PushSource;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.project.Project;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public final class GitPushDialog extends VcsPushDialog {
  @UIEffect
  public GitPushDialog(Project project,
      Collection<? extends Repository> allRepos, java.util.List<? extends Repository> selectedRepositories,
      @org.jetbrains.annotations.Nullable Repository currentRepo, @org.jetbrains.annotations.Nullable PushSource pushSource,
      Boolean isForcePushRequired) {
    super(project, allRepos, selectedRepositories, currentRepo, pushSource);
    setOKButtonText(getPushActionName(isForcePushRequired));
    setTitle("Push Commits");
    init();
  }

  private String getPushActionName(boolean isForcePushRequired) {
    return isForcePushRequired ? "Force _Push" : "_Push";
  }

}
