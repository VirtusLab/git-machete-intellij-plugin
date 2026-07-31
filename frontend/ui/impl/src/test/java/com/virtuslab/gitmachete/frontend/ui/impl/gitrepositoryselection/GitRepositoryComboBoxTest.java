package com.virtuslab.gitmachete.frontend.ui.impl.gitrepositoryselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class GitRepositoryComboBoxTest {

  @Test
  public void availableRepositoriesExcludeRootsWithoutGitDirectory() {
    Project project = mock(Project.class);
    GitRepository availableRepository = mock(GitRepository.class);
    GitRepository removedRepository = mock(GitRepository.class);
    VirtualFile availableRoot = mock(VirtualFile.class);
    VirtualFile removedRoot = mock(VirtualFile.class);
    VirtualFile gitDirectory = mock(VirtualFile.class);

    when(availableRepository.getRoot()).thenReturn(availableRoot);
    when(removedRepository.getRoot()).thenReturn(removedRoot);

    try (MockedStatic<GitUtil> gitUtil = mockStatic(GitUtil.class)) {
      gitUtil.when(() -> GitUtil.getRepositories(project))
          .thenReturn(Arrays.asList(availableRepository, removedRepository));
      gitUtil.when(() -> GitUtil.findGitDir(availableRoot)).thenReturn(gitDirectory);
      gitUtil.when(() -> GitUtil.findGitDir(removedRoot)).thenReturn(null);

      assertEquals(
          List.of(availableRepository),
          GitRepositoryComboBox.getAvailableRepositories(project));
    }
  }

  @Test
  public void onlyGitDirectoryDeletionIsDetected() {
    VirtualFile gitDirectory = mock(VirtualFile.class);
    when(gitDirectory.getName()).thenReturn(".git");
    VirtualFile otherDirectory = mock(VirtualFile.class);
    when(otherDirectory.getName()).thenReturn("other");

    VFileDeleteEvent deletion = mock(VFileDeleteEvent.class);
    when(deletion.getFile()).thenReturn(gitDirectory);
    VFileDeleteEvent otherDeletion = mock(VFileDeleteEvent.class);
    when(otherDeletion.getFile()).thenReturn(otherDirectory);

    VFileContentChangeEvent contentChange = mock(VFileContentChangeEvent.class);
    when(contentChange.getFile()).thenReturn(gitDirectory);

    assertTrue(GitRepositoryComboBox.isGitDirectoryDeletion(deletion));
    assertFalse(GitRepositoryComboBox.isGitDirectoryDeletion(otherDeletion));
    assertFalse(GitRepositoryComboBox.isGitDirectoryDeletion(contentChange));
  }
}
