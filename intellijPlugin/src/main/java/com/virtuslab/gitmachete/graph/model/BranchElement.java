package com.virtuslab.gitmachete.graph.model;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public final class BranchElement implements GraphElement {
  @Getter private final IGitMacheteBranch branch;

  @Override
  public String getValue() {
    return branch.getName();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BranchElement && this.branch.equals(((BranchElement) o).branch);
  }
}
