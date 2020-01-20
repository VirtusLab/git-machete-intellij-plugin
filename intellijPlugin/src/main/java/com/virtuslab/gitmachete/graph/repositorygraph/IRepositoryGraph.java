package com.virtuslab.gitmachete.graph.repositorygraph;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.Collection;

public interface IRepositoryGraph extends LinearGraph {
  Collection<? extends PrintElement> getPrintElements(int rowIndex);

  IGraphElement getGraphElement(int rowIndex);
}
