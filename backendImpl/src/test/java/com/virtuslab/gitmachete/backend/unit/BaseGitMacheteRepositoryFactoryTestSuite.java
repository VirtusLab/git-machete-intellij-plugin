package com.virtuslab.gitmachete.backend.unit;

import java.util.Arrays;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

public class BaseGitMacheteRepositoryFactoryTestSuite {

  private static final Class<?> AUX_CLASS = getAuxClass();

  @SneakyThrows
  private static Class<?> getAuxClass() {
    return Whitebox.getInnerClassType(GitMacheteRepositoryFactory.class, "CreateGitMacheteRepositoryAux");
  }

  protected final IGitCoreRepository gitCoreRepository = PowerMockito.mock(IGitCoreRepository.class);

  @SneakyThrows
  protected Object aux(IGitCoreBranch... localCoreBranches) {
    PowerMockito.doReturn(List.ofAll(Arrays.stream(localCoreBranches))).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();

    return Whitebox
        .getConstructor(AUX_CLASS, IGitCoreRepository.class, StatusBranchHookExecutor.class, PreRebaseHookExecutor.class)
        .newInstance(gitCoreRepository, /* statusBranchHookExecutor */ null, /* preRebaseHookExecutor */ null);
  }
}
