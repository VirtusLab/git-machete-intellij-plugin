package com.virtuslab.gitmachete.frontend.ui.api.table;

import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;

public interface ISimpleGraphTableProvider {
  @UIEffect
  BaseGraphTable deriveInstance(IGitMacheteRepositorySnapshot macheteRepositorySnapshot);

  @UIEffect
  BaseGraphTable deriveDemoInstance();
}
