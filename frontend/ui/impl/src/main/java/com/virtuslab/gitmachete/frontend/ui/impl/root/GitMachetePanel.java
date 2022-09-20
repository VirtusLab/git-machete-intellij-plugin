package com.virtuslab.gitmachete.frontend.ui.impl.root;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.AncestorListenerAdapter;
import com.intellij.ui.ScrollPaneFactory;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.defs.ActionGroupIds;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.defs.FileTypeIds;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.impl.RediscoverSuggester;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod(GitVfsUtils.class)
@CustomLog
public final class GitMachetePanel extends SimpleToolWindowPanel {

  private final Project project;

  @UIEffect
  public GitMachetePanel(Project project) {
    super(/* vertical */ false, /* borderless */ true);
    LOG.debug("Instantiating");
    this.project = project;

    val selectedGitRepositoryProvider = project.getService(SelectedGitRepositoryProvider.class);
    val selectionComponent = selectedGitRepositoryProvider.getSelectionComponent();
    val graphTable = getGraphTable();

    val messageBusConnection = project.getMessageBus().connect();
    messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(java.util.List<? extends VFileEvent> events) {
        for (val event : events) {
          if (event instanceof VFileContentChangeEvent) {
            if (((VFileContentChangeEvent) event).getFile().getFileType().getName().equals(FileTypeIds.NAME)) {
              graphTable.queueRepositoryUpdateAndModelRefresh();
            }
          }
        }
      }
    });
    Disposer.register(project, messageBusConnection);

    // This class is final, so the instance is `@Initialized` at this point.

    setToolbar(createGitMacheteVerticalToolbar(graphTable).getComponent());
    add(createShrinkingWrapper(selectionComponent), BorderLayout.NORTH);
    setContent(ScrollPaneFactory.createScrollPane(graphTable));

    // The following listener executes on each opening of the Git Machete tab.
    addAncestorListener(new AncestorListenerAdapter() {
      @Override
      public void ancestorAdded(AncestorEvent event) {
        val gitRepository = selectedGitRepositoryProvider.getSelectedGitRepository();
        if (gitRepository != null) {
          val macheteFilePath = gitRepository.getMacheteFilePath();
          Runnable queueDiscoverOperation = () -> graphTable.queueDiscover(macheteFilePath, () -> {});
          val rediscoverSuggester = new RediscoverSuggester(gitRepository, queueDiscoverOperation);
          graphTable.queueRepositoryUpdateAndModelRefresh(rediscoverSuggester::perform);
        }
      }
    });
  }

  public BaseEnhancedGraphTable getGraphTable() {
    return project.getService(GraphTableProvider.class).getGraphTable();
  }

  @UIEffect
  private static ActionToolbar createGitMacheteVerticalToolbar(BaseEnhancedGraphTable graphTable) {
    val actionManager = ActionManager.getInstance();
    val toolbarActionGroup = (ActionGroup) actionManager.getAction(ActionGroupIds.ACTION_GROUP_TOOLBAR);
    val toolbar = actionManager.createActionToolbar(ActionPlaces.ACTION_PLACE_TOOLBAR, toolbarActionGroup,
        /* horizontal */ false);
    toolbar.setTargetComponent(graphTable);
    return toolbar;
  }

  @UIEffect
  private static JComponent createShrinkingWrapper(JComponent component) {
    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(component, BorderLayout.WEST);
    wrapper.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    return wrapper;
  }
}
