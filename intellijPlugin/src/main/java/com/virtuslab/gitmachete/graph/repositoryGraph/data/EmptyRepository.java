package com.virtuslab.gitmachete.graph.repositoryGraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Getter;

public class EmptyRepository implements IGitMacheteRepository {
  @Getter @Nonnull private static EmptyRepository instance = new EmptyRepository();

  @Override
  public List<IGitMacheteBranch> getRootBranches() {
    return Collections.emptyList();
  }

  @Override
  public void addRootBranch(IGitMacheteBranch branch) {}

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranch() {
    return Optional.empty();
  }

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.empty();
  }

  @Override
  public Map<String, IGitMacheteRepository> getSubmoduleRepositories() {
    return null;
  }
}
