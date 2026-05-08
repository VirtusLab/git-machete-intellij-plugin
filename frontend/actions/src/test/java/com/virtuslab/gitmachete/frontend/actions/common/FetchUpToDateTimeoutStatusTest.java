package com.virtuslab.gitmachete.frontend.actions.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRepository;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FetchUpToDateTimeoutStatusTest {

  @BeforeEach
  public void setUp() {
    FetchUpToDateTimeoutStatus.clear();
  }

  private static GitRepository mockRepo(String rootPath) {
    val name = rootPath.substring(rootPath.lastIndexOf('/') + 1);
    val virtualFile = mock(VirtualFile.class);
    when(virtualFile.getPath()).thenReturn(rootPath);
    when(virtualFile.getName()).thenReturn(name);
    val gitRepository = mock(GitRepository.class);
    when(gitRepository.getRoot()).thenReturn(virtualFile);
    return gitRepository;
  }

  @Test
  public void shouldNotConsiderFreshRepoUpToDate() {
    val repo = mockRepo("/home/user/work/projA/api");
    assertFalse(FetchUpToDateTimeoutStatus.isUpToDate(repo));
  }

  @Test
  public void shouldConsiderJustUpdatedRepoUpToDate() {
    val repo = mockRepo("/home/user/work/projA/api");
    FetchUpToDateTimeoutStatus.update(repo);
    assertTrue(FetchUpToDateTimeoutStatus.isUpToDate(repo));
  }

  @Test
  public void shouldNotShareStateBetweenReposSharingDirectoryName() {
    val repoA = mockRepo("/home/user/work/projA/api");
    val repoB = mockRepo("/home/user/work/projB/api");
    assertEquals(repoA.getRoot().getName(), repoB.getRoot().getName());

    FetchUpToDateTimeoutStatus.update(repoA);

    assertTrue(FetchUpToDateTimeoutStatus.isUpToDate(repoA));
    assertFalse(FetchUpToDateTimeoutStatus.isUpToDate(repoB));
  }
}
