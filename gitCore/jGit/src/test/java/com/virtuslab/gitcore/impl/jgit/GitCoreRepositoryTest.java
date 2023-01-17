package com.virtuslab.gitcore.impl.jgit;

import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.SneakyThrows;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
public class GitCoreRepositoryTest {

  private final GitCoreRepository gitCoreRepository = Whitebox.newInstance(GitCoreRepository.class);

  @BeforeEach
  @SneakyThrows
  public void mockGitCoreRepository() {
    PowerMockito.stub(PowerMockito.method(RevWalk.class, "parseCommit")).toThrow(new Exception("Mock"));
    PowerMockito.stub(PowerMockito.method(GitCoreRepository.class, "convertRevisionToObjectId"))
        .toReturn(Whitebox.newInstance(ObjectId.class));
  }

  @Test
  public void shouldContainExceptionsInsideOptionReturningMethods() throws Exception {
    assertNull(gitCoreRepository.parseRevision(""));
  }
}
