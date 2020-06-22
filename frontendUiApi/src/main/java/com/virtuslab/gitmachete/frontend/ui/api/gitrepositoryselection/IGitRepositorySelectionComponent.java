package com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection;

import javax.swing.JComponent;

public interface IGitRepositorySelectionComponent extends IGitRepositorySelectionProvider {
  JComponent getSelectionComponent();
}
