package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.val;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.common.SideEffectingActionTrackingService;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public abstract class SideEffectingBackgroundable extends Task.Backgroundable {

  // Task.Backgroundable already brings in a protected `myProject`, but it's @Nullable,
  // which forces unnecessary null checks to satisfy static analysis.
  protected final Project project;
  private final @Untainted String shortName;

  public SideEffectingBackgroundable(Project project, String title, @Untainted String shortName) {
    super(project, title);
    this.project = project;
    this.shortName = shortName;
  }

  @Override
  @UIThreadUnsafe
  public final void run(ProgressIndicator indicator) {
    try (val ignored = project.getService(SideEffectingActionTrackingService.class).register(shortName)) {
      doRun(indicator);
    }
  }

  @UIThreadUnsafe
  protected abstract void doRun(ProgressIndicator indicator);
}
