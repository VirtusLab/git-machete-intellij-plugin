package com.virtuslab.gitmachete.graph.repositorygraph;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.virtuslab.gitmachete.graph.model.GraphElementI;
import java.util.Collection;

public interface IRepositoryGraph extends LinearGraph {
  Collection<? extends PrintElement> getPrintElements(int rowIndex);

  GraphElementI getGraphElement(int rowIndex);
}
