package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Getter;

public final class NullRepository implements IGitMacheteRepository {
  @Getter @Nonnull private static final NullRepository instance = new NullRepository();

  @Override
  public List<IGitMacheteBranch> getRootBranches() {
    return Collections.emptyList();
  }

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.empty();
  }

  @Override
  public Optional<IGitMacheteBranch> getBranchByName(String branchName) {
    return Optional.empty();
  }

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.empty();
  }

  @Override
  public List<IGitMacheteSubmoduleEntry> getSubmodules() {
    return Collections.emptyList();
  }

  @Override
  public IBranchRelationFile getBranchRelationFile() {
    return null;
  }

  @Override
  public IGitMacheteRepository withBranchRelationFile(IBranchRelationFile branchRelationFile) {
    return this;
  }
}
