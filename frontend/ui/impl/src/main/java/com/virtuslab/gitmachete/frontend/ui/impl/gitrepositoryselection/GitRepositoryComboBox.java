package com.virtuslab.gitmachete.frontend.ui.impl.gitrepositoryselection;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.MutableCollectionComboBoxModel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ModalityUiUtil;
import com.intellij.util.SmartList;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionChangeObserver;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponentProvider;

@CustomLog
public final class GitRepositoryComboBox extends JComboBox<GitRepository>
    implements
      Disposable,
      IGitRepositorySelectionComponentProvider {

  private final java.util.List<IGitRepositorySelectionChangeObserver> observers = new SmartList<>();

  private final Project project;

  @UIEffect
  public GitRepositoryComboBox(Project project) {
    super(new MutableCollectionComboBoxModel<>());
    this.project = project;

    updateRepositories();
    setRenderer(SimpleListCellRenderer.create( /* nullValue */ "", DvcsUtil::getShortRepositoryName));

    val messageBusConnection = project.getMessageBus().connect();
    messageBusConnection
        .subscribe(VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED, () -> {
          LOG.debug("Git repository mappings changed");
          ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, () -> updateRepositories());
        });
    Disposer.register(this, messageBusConnection);
  }

  @Override
  @SafeEffect
  public MutableCollectionComboBoxModel<GitRepository> getModel() {
    return (MutableCollectionComboBoxModel<GitRepository>) super.getModel();
  }

  @UIEffect
  private void updateRepositories() {
    val repositories = List.ofAll(GitUtil.getRepositories(project));
    LOG.debug("Git repositories:");
    repositories.forEach(r -> LOG.debug("* ${r.getRoot().getName()}"));

    // `com.intellij.ui.MutableCollectionComboBoxModel.getSelected` must be performed
    // before `com.intellij.ui.MutableCollectionComboBoxModel.update`
    // because the update method sets the selected item to null
    val selected = getModel().getSelected();
    if (!getModel().getItems().equals(repositories)) {
      getModel().update(DvcsUtil.sortRepositories(repositories.asJavaMutable()));
    }

    this.setVisible(getModel().getItems().size() > 1);

    val selectedItemUpdateRequired = selected == null || !getModel().getItems().contains(selected);
    if (repositories.isEmpty()) {
      // TODO (#255): properly handle plugin visibility/"empty" text on no-repo project
      LOG.debug("No Git repositories found");
      if (selected != null) {
        setSelectedItem(null);
      }
    } else if (selectedItemUpdateRequired) {
      LOG.debug("Selecting first Git repository");
      setSelectedItem(repositories.get(0));
    } else {
      LOG.debug("Selecting previously selected Git repository");
      // GitRepositoryComboBox#setSelectedItem is omitted to avoid unnecessary observers call
      getModel().setSelectedItem(selected);
    }
  }

  @Override
  public @Nullable GitRepository getSelectedGitRepository() {
    return getModel().getSelected();
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

  @Override
  public JComponent getSelectionComponent() {
    return this;
  }

  @Override
  public void dispose() {

  }
}
