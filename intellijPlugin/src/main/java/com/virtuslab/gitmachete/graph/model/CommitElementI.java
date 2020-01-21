package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public final class CommitElementI implements GraphElementI {
  @Getter private final IGitMacheteCommit commit;

  @Override
  public String getValue() {
    return commit.getMessage();
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return SimpleTextAttributes.GRAYED_ATTRIBUTES;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof CommitElementI && ((CommitElementI) obj).commit.equals(commit);
  }
}
