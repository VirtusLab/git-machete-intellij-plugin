package com.virtuslab.gitmachete.frontend.ui.api.table;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;

public interface IGitMacheteRepositorySnapshotProvider {
  @Nullable
  IGitMacheteRepositorySnapshot getGitMacheteRepositorySnapshot();
}
