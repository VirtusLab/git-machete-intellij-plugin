package com.virtuslab.gitcore.impl.jgit;

import lombok.SneakyThrows;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitCoreRepository.class, RevWalk.class})
public class GitCoreRepositoryTest {

  private final GitCoreRepository gitCoreRepository = Whitebox.newInstance(GitCoreRepository.class);

  @Before
  @SneakyThrows
  public void mockGitCoreRepository() {
    PowerMockito.stub(PowerMockito.method(RevWalk.class, "parseCommit")).toThrow(new Exception("Mock"));
    PowerMockito.stub(PowerMockito.method(GitCoreRepository.class, "convertRevisionToObjectId"))
        .toReturn(Whitebox.newInstance(ObjectId.class));
  }

  @Test
  public void shouldContainExceptionsInsideOptionReturningMethods() throws Exception {
    Assert.assertNull(gitCoreRepository.parseRevision(""));
  }
}
