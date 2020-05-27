package com.virtuslab.gitmachete.backend.unit;

import java.util.Arrays;

import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

public class BaseGitMacheteRepositoryFactoryTest {

  protected static final Class<?> AUX_CLASS = Try.of(() -> Whitebox
      .getInnerClassType(GitMacheteRepositoryFactory.class, "Aux")).get();

  protected final IGitCoreRepository gitCoreRepository = PowerMockito.mock(IGitCoreRepository.class);

  @SneakyThrows
  protected Object aux(IGitCoreBranch... localCoreBranches) {
    PowerMockito.doReturn(List.ofAll(Arrays.stream(localCoreBranches))).when(gitCoreRepository).deriveAllLocalBranches();
    PowerMockito.doReturn(List.empty()).when(gitCoreRepository).deriveAllRemoteBranches();

    return Whitebox
        .getConstructor(AUX_CLASS, IGitCoreRepository.class, StatusBranchHookExecutor.class)
        .newInstance(gitCoreRepository, /* statusBranchHookExecutor */ null);
  }

}
