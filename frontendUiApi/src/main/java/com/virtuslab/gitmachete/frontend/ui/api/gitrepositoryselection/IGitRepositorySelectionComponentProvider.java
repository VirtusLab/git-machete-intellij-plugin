package com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection;

import javax.swing.JComponent;

public interface IGitRepositorySelectionComponentProvider extends IGitRepositorySelectionProvider {
  JComponent getSelectionComponent();
}
