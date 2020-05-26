package com.virtuslab.gitmachete.backend.unit;

import io.vavr.control.Try;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;

public class BaseGitMacheteRepositoryFactoryTest {

  protected static final Class<?> AUX_CLASS = Try.of(() -> Whitebox
          .getInnerClassType(GitMacheteRepositoryFactory.class, "Aux")).get();

  protected final IGitCoreRepository gitCoreRepository = PowerMockito.mock(IGitCoreRepository.class);

  protected final Object aux = Try.of(() -> Whitebox
          .getConstructor(AUX_CLASS, IGitCoreRepository.class, StatusBranchHookExecutor.class)
          .newInstance(gitCoreRepository, /* statusBranchHookExecutor */ null)).get();

}
