package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.commons.lang.NotImplementedException;

public class NullRepository implements IGitMacheteRepository {
  @Getter @Nonnull private static NullRepository instance = new NullRepository();

  @Override
  public List<IGitMacheteBranch> getRootBranches() {
    return Collections.emptyList();
  }

  @Override
  public void addRootBranch(IGitMacheteBranch branch) {
    throw new NotImplementedException();
  }

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranch() {
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
}
