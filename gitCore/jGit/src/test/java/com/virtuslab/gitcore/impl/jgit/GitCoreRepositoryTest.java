package com.virtuslab.gitcore.impl.jgit;

/*
 * this test seems to be irrelevant. because the method is no longer returning an Option.


@RunWith(PowerMockRunner.class)
@PrepareForTest({GitCoreRepository.class, RevWalk.class})
public class GitCoreRepositoryTest {

  private final GitCoreRepository gitCoreRepository = Whitebox.newInstance(GitCoreRepository.class);

  @Before
  @SneakyThrows
  public void mockGitCoreRepository() {
    PowerMockito.stub(PowerMockito.method(RevWalk.class, "parseCommit")).toThrow(new Exception("Mock"));
    PowerMockito.stub(PowerMockito.method(GitCoreRepository.class, "convertRevisionToObjectId"))
        .toReturn(Option.some(Whitebox.newInstance(ObjectId.class)));
  }

    @Test
    public void shouldContainExceptionsInsideOptionReturningMethods() throws Exception {

      Assert.assertEquals(gitCoreRepository.parseRevision(""), null);
    }
}

 */
