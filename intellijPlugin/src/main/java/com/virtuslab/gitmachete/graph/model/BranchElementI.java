package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public final class BranchElementI implements GraphElementI {
  @Getter private final IGitMacheteBranch branch;

  @Override
  public String getValue() {
    return branch.getName();
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BranchElementI && this.branch.equals(((BranchElementI) o).branch);
  }
}
