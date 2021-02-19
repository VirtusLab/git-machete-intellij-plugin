package com.virtuslab.gitmachete.backend.unit;

import java.nio.file.Paths;
import java.util.Arrays;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreHeadSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepository;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

public class BaseGitMacheteRepositoryUnitTestSuite {

  private static final Class<?> AUX_CLASS = getAuxClass();

  @SneakyThrows
  private static Class<?> getAuxClass() {
    return Whitebox.getInnerClassType(GitMacheteRepository.class, "CreateGitMacheteRepositoryAux");
  }

  protected final IGitCoreRepository gitCoreRepository = PowerMockito.mock(IGitCoreRepository.class);

  @SneakyThrows
  protected Object aux(IGitCoreBranchSnapshot... localCoreBranches) {
    PowerMockito.doReturn(List.ofAll(Arrays.stream(localCoreBranches))).when(gitCoreRepository).deriveAllLocalBranches();

    var iGitCoreHeadSnapshot = PowerMockito.mock(IGitCoreHeadSnapshot.class);
    PowerMockito.doReturn(Option.none()).when(iGitCoreHeadSnapshot).getTargetBranch();
    PowerMockito.doReturn(iGitCoreHeadSnapshot).when(gitCoreRepository).deriveHead();
    PowerMockito.doReturn(Option.none()).when(gitCoreRepository).deriveConfigValue("core", "hooksPath");

    // cannot be mocked as it is final
    var statusBranchHookExecutor = new StatusBranchHookExecutor(gitCoreRepository, Paths.get("void"), Paths.get("void"));

    return Whitebox
        .getConstructor(AUX_CLASS, IGitCoreRepository.class, StatusBranchHookExecutor.class, PreRebaseHookExecutor.class)
        .newInstance(gitCoreRepository, statusBranchHookExecutor, /* preRebaseHookExecutor */ null);
  }
}
