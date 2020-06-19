package com.virtuslab.gitmachete.frontend.ui.api.vcsrootcombobox;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.intellij.ui.MutableCollectionComboBoxModel;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public abstract class BaseVcsRootComboBox extends JComboBox<GitRepository> implements IGitRepositorySelectionProvider {

  @UIEffect
  public BaseVcsRootComboBox(MutableCollectionComboBoxModel<GitRepository> model) {
    super(model);
  }

  @UIEffect
  public static JComponent createShrinkingWrapper(JComponent component) {
    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(component, BorderLayout.WEST);
    wrapper.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    return wrapper;
  }
}
