package com.virtuslab.gitmachete.graph.repositoryGraph;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.virtuslab.gitmachete.graph.model.GraphElement;
import java.util.Collection;

public interface IRepositoryGraph extends LinearGraph {
  Collection<? extends PrintElement> getPrintElements(int rowIndex);

  GraphElement getGraphElement(int rowIndex);
}
