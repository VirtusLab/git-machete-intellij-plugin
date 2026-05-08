package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRepository;
import lombok.val;
import org.junit.jupiter.api.Test;

public class UnmanagedBranchNotificationFactoryTest {

  private static GitRepository mockRepo(String rootPath) {
    val virtualFile = mock(VirtualFile.class);
    when(virtualFile.getPath()).thenReturn(rootPath);
    val gitRepository = mock(GitRepository.class);
    when(gitRepository.getRoot()).thenReturn(virtualFile);
    return gitRepository;
  }

  @Test
  public void propertyKeyShouldDifferBetweenReposSharingDirectoryName() {
    val repoA = mockRepo("/home/user/work/projA/api");
    val repoB = mockRepo("/home/user/work/projB/api");

    val keyA = UnmanagedBranchNotificationFactory.propertyKeyForBranch(repoA, "master");
    val keyB = UnmanagedBranchNotificationFactory.propertyKeyForBranch(repoB, "master");

    assertNotEquals(keyA, keyB);
  }
}
