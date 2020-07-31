package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.ui.table.JBTable;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;

public interface ISimpleGraphTableProvider {
  @UIEffect
  JBTable deriveInstance(IGitMacheteRepositorySnapshot macheteRepositorySnapshot);

  @UIEffect
  JBTable deriveDemoInstance();
}
