package com.virtuslab.gitmachete.frontend.ui.impl.root;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.intellij.ui.MutableCollectionComboBoxModel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.SmartList;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.common.value.qual.MinLen;

import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionChangeObserver;
import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;

public final class VcsRootComboBox extends JComboBox<GitRepository> implements IGitRepositorySelectionProvider {

  private final java.util.List<IGitRepositorySelectionChangeObserver> observers = new SmartList<>();

  /**
   * @param repositories non-empty list of {@link git4idea.repo.GitRepository} that represents VCS repositories
   */
  @UIEffect
  public VcsRootComboBox(@MinLen(1) List<GitRepository> repositories) {
    super(new MutableCollectionComboBoxModel<>(/* items */ repositories.asJavaMutable(),
        /* selection */ repositories.get(0)));
    setRenderer(SimpleListCellRenderer.create("", repo -> repo.getRoot().getName()));
  }

  @Override
  public MutableCollectionComboBoxModel<GitRepository> getModel() {
    return (MutableCollectionComboBoxModel<GitRepository>) super.getModel();
  }

  @Override
  @UIEffect
  public void updateRepositories(@MinLen(1) List<GitRepository> repositories) {
    // `com.intellij.ui.CollectionComboBoxModel.getSelected` must be performed
    // before `com.intellij.ui.MutableCollectionComboBoxModel.update`
    // because the update method sets selected item to null
    GitRepository selected = getModel().getSelected();
    if (!getModel().getItems().equals(repositories)) {
      getModel().update(repositories.asJavaMutable());
    }

    boolean selectedItemUpdateRequired = !getModel().getItems().contains(selected);
    if (selectedItemUpdateRequired) {
      getModel().setSelectedItem(repositories.get(0));
    } else {
      getModel().setSelectedItem(selected);
    }

    this.setVisible(getModel().getItems().size() > 1);
  }

  @Override
  public GitRepository getSelectedRepository() {
    return getModel().getSelected();
  }

  @Override
  @UIEffect
  public void setSelectedItem(Object anObject) {
    super.setSelectedItem(anObject);
    observers.forEach(o -> o.onSelectionChanged());
  }

  public void addSelectionChangeObserver(IGitRepositorySelectionChangeObserver observer) {
    observers.add(observer);
  }

  @UIEffect
  public static JComponent createShrinkingWrapper(JComponent component) {
    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(component, BorderLayout.WEST);
    wrapper.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    return wrapper;
  }
}
