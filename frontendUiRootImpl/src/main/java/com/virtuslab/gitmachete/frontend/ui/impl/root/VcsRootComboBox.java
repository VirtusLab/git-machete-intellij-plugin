package com.virtuslab.gitmachete.frontend.ui.impl.root;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.MutableCollectionComboBoxModel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.SmartList;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionChangeObserver;
import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;

@CustomLog
public final class VcsRootComboBox extends JComboBox<GitRepository> implements IGitRepositorySelectionProvider {

  private final java.util.List<IGitRepositorySelectionChangeObserver> observers = new SmartList<>();

  private final Project project;

  @UIEffect
  public VcsRootComboBox(Project project) {
    super(new MutableCollectionComboBoxModel<>());
    this.project = project;

    updateRepositories();
    setRenderer(SimpleListCellRenderer.create( /* nullValue */ "", repo -> repo.getRoot().getName()));

    project.getMessageBus().connect()
        .subscribe(VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED, () -> {
          LOG.debug("VCS repository roots mappings changed");
          GuiUtils.invokeLaterIfNeeded(() -> updateRepositories(), ModalityState.NON_MODAL);
        });
  }

  @Override
  public MutableCollectionComboBoxModel<GitRepository> getModel() {
    return (MutableCollectionComboBoxModel<GitRepository>) super.getModel();
  }

  @UIEffect
  private void updateRepositories() {
    // A bit of a shortcut: we're accessing filesystem even though we are on UI thread here;
    // this shouldn't ever be a heavyweight operation, however.
    List<GitRepository> repositories = List.ofAll(GitUtil.getRepositories(project));
    LOG.debug(() -> "VCS roots:");
    repositories.forEach(r -> LOG.debug("* ${r.getRoot().getName()}"));

    // `com.intellij.ui.CollectionComboBoxModel.getSelected` must be performed
    // before `com.intellij.ui.MutableCollectionComboBoxModel.update`
    // because the update method sets the selected item to null
    GitRepository selected = getModel().getSelected();
    if (!getModel().getItems().equals(repositories)) {
      getModel().update(DvcsUtil.sortRepositories(repositories.asJavaMutable()));
    }

    this.setVisible(getModel().getItems().size() > 1);

    boolean selectedItemUpdateRequired = selected == null || !getModel().getItems().contains(selected);
    if (repositories.isEmpty()) {
      // TODO (#255): properly handle plugin visibility/"empty" text on no-repo project
      LOG.debug(() -> "No VCS roots found");
      if (selected != null) {
        setSelectedItem(null);
      }
    } else if (selectedItemUpdateRequired) {
      LOG.debug(() -> "Selecting first VCS root");
      setSelectedItem(repositories.get(0));
    } else {
      LOG.debug(() -> "Selecting previously selected VCS root");
      // VcsRootComboBox#setSelectedItem is omitted to avoid unnecessary observers call
      getModel().setSelectedItem(selected);
    }
  }

  @Override
  @UIEffect
  public Option<GitRepository> getSelectedRepository() {
    return Option.of(getModel().getSelected());
  }

  @Override
  @UIEffect
  public void setSelectedItem(@Nullable Object anObject) {
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
