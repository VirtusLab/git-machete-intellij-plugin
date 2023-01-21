package com.virtuslab.gitmachete.backend.unit;

import java.nio.file.Paths;
import java.util.Arrays;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.powermock.api.mockito.PowerMockito;

import com.virtuslab.gitcore.api.IGitCoreBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreHeadSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.impl.aux.CreateGitMacheteRepositoryAux;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

public class BaseGitMacheteRepositoryUnitTestSuite {

  protected final IGitCoreRepository gitCoreRepository = PowerMockito.mock(IGitCoreRepository.class);

  @SneakyThrows
  protected Object aux(IGitCoreBranchSnapshot... localCoreBranches) {
    PowerMockito.doReturn(List.ofAll(Arrays.stream(localCoreBranches))).when(gitCoreRepository).deriveAllLocalBranches();

    val iGitCoreHeadSnapshot = PowerMockito.mock(IGitCoreHeadSnapshot.class);
    PowerMockito.doReturn(null).when(iGitCoreHeadSnapshot).getTargetBranch();
    PowerMockito.doReturn(iGitCoreHeadSnapshot).when(gitCoreRepository).deriveHead();
    PowerMockito.doReturn(null).when(gitCoreRepository).deriveConfigValue("core", "hooksPath");
    PowerMockito.doReturn(Paths.get("void")).when(gitCoreRepository).getRootDirectoryPath();
    PowerMockito.doReturn(Paths.get("void")).when(gitCoreRepository).getMainGitDirectoryPath();

    // cannot be mocked as it is final
    val statusBranchHookExecutor = StatusBranchHookExecutor.of(gitCoreRepository);

    return new CreateGitMacheteRepositoryAux(gitCoreRepository, statusBranchHookExecutor, /* preRebaseHookExecutor */ null);
  }
}
