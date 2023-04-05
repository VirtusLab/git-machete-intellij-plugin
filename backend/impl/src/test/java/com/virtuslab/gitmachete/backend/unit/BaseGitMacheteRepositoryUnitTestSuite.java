package com.virtuslab.gitmachete.backend.unit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.Arrays;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;

import com.virtuslab.gitcore.api.IGitCoreHeadSnapshot;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.impl.aux.CreateGitMacheteRepositoryAux;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

public class BaseGitMacheteRepositoryUnitTestSuite {

  protected final IGitCoreRepository gitCoreRepository = mock(IGitCoreRepository.class);

  @SneakyThrows
  protected CreateGitMacheteRepositoryAux aux(IGitCoreLocalBranchSnapshot... localCoreBranches) {
    when(gitCoreRepository.deriveAllLocalBranches()).thenReturn(List.ofAll(Arrays.stream(localCoreBranches)));

    val iGitCoreHeadSnapshot = mock(IGitCoreHeadSnapshot.class);
    when(iGitCoreHeadSnapshot.getTargetBranch()).thenReturn(null);
    when(gitCoreRepository.deriveHead()).thenReturn(iGitCoreHeadSnapshot);
    when(gitCoreRepository.deriveConfigValue("core", "hooksPath")).thenReturn(null);
    when(gitCoreRepository.getRootDirectoryPath()).thenReturn(Paths.get("void"));
    when(gitCoreRepository.getMainGitDirectoryPath()).thenReturn(Paths.get("void"));

    // cannot be mocked as it is final
    val statusBranchHookExecutor = new StatusBranchHookExecutor(gitCoreRepository);

    return new CreateGitMacheteRepositoryAux(gitCoreRepository, statusBranchHookExecutor, /* preRebaseHookExecutor */ null);
  }
}
