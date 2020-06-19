package com.virtuslab.gitmachete.frontend.ui.api.repositoryselection;

import javax.swing.JComponent;

public interface ISelectionComponent extends IGitRepositorySelectionProvider {
  JComponent getComponent();
}
