package com.virtuslab.gitmachete.frontend.ui.api.table;

import java.nio.file.Path;

import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public interface IGraphTable {
  @UIEffect
  boolean isListingCommits();

  @UIEffect
  void setListingCommits(boolean isListingCommits);

  @UIEffect
  void refreshModel();

  @UIEffect
  void refreshModel(@Nullable IGitMacheteRepository gitMacheteRepository, Path macheteFilePath, boolean isMacheteFilePresent);

  void setBranchLayoutWriter(IBranchLayoutWriter branchLayoutWriter);
}
