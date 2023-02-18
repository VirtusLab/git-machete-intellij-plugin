package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Objects;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(Objects.class)
public abstract class InferParentForUnmanagedBranchBackgroundable extends Task.Backgroundable {

  private final Project project;

  public InferParentForUnmanagedBranchBackgroundable(Project project) {
    super(project, getString("string.GitMachete.InferParentForUnmanagedBranchBackgroundable.task-title"));
    this.project = project;
  }

  @UIThreadUnsafe
  protected abstract @Nullable ILocalBranchReference inferParent() throws GitMacheteException;

  protected abstract void onInferParentSuccess(ILocalBranchReference inferredParent);

  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    try {
      val inferredParent = inferParent();
      if (inferredParent != null) {
        onInferParentSuccess(inferredParent);
      }
    } catch (GitMacheteException e) {
      VcsNotifier.getInstance(project)
          .notifyError(
              /* displayId */ null,
              getString(
                  "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
              e.getMessage().requireNonNullElse(""));
    }
  }
}
