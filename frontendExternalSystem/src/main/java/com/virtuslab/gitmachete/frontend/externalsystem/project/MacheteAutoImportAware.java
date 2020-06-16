package com.virtuslab.gitmachete.frontend.externalsystem.project;

import java.io.File;
import java.util.Objects;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@Service
public final class MacheteAutoImportAware implements ExternalSystemAutoImportAware {

  @Override
  public @Nullable String getAffectedExternalProjectPath(String changedFileOrDirPath, Project project) {
    return null;
  }

  @Override
  public java.util.List<File> getAffectedExternalProjectFiles(String projectPath, Project project) {
    return List.ofAll(GitUtil.getRepositories(project))
        .map(Objects::toString)
        .map(path -> path.concat("/.git/machete"))
        .map(File::new)
        .collect(Collectors.toList());
  }
}
