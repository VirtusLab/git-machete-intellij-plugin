package com.virtuslab.gitmachete.api;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import java.util.Collection;

public interface IRepositoryGraph extends LinearGraph {
  Collection<? extends PrintElement> getPrintElements(int rowIndex);

  IGitMacheteBranch getBranch(int rowIndex);
}
