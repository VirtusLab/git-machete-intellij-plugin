package com.virtuslab.gitmachete.frontend.externalsystem.project;

import java.io.File;
import java.util.Objects;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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
  @SuppressWarnings("nullness")
  public java.util.List<File> getAffectedExternalProjectFiles(String projectPath, Project project) {
    return List.ofAll(GitUtil.getRepositories(project))
        .map(repository -> GitUtil.findGitDir(repository.getRoot()))
        .filter(Objects::nonNull)
        .map(gitDirectory -> gitDirectory.findChild("machete"))
        .filter(Objects::nonNull)
        .map(VirtualFile::getPath)
        .map(File::new)
        .toJavaList();
  }
}
