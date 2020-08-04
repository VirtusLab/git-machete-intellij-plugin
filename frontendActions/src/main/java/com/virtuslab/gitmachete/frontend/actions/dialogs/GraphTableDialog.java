package com.virtuslab.gitmachete.frontend.actions.dialogs;

import javax.swing.Action;
import javax.swing.JComponent;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.ui.api.table.ISimpleGraphTableProvider;

@UI
public final class GraphTableDialog extends DialogWrapper {
  private final JBTable table;
  private final @Nullable IGitMacheteRepositorySnapshot repositorySnapshot;
  private final int windowWidth;
  private final int windowHeight;
  private final @Nullable Consumer<IGitMacheteRepositorySnapshot> okAction;
  private final boolean cancelButtonVisible;

  public static GraphTableDialog of(IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot, String windowTitle,
      @Nullable String emptyTableText, @Nullable Consumer<IGitMacheteRepositorySnapshot> okAction, String okButtonText,
      boolean cancelButtonVisible) {
    return of(gitMacheteRepositorySnapshot, windowTitle, /* windowWidth */ 800, /* windowHeight */ 500, emptyTableText,
        okAction, okButtonText, cancelButtonVisible);
  }

  public static GraphTableDialog of(IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot, String windowTitle,
      int windowWidth, int windowHeight, @Nullable String emptyTableText,
      @Nullable Consumer<IGitMacheteRepositorySnapshot> okAction, String okButtonText, boolean cancelButtonVisible) {
    var table = RuntimeBinding.instantiateSoleImplementingClass(ISimpleGraphTableProvider.class)
        .deriveInstance(gitMacheteRepositorySnapshot);
    table.setTextForEmptyTable(emptyTableText != null ? emptyTableText : "");
    return new GraphTableDialog(table, gitMacheteRepositorySnapshot, windowTitle, windowWidth, windowHeight, okAction,
        okButtonText, cancelButtonVisible);
  }

  public static GraphTableDialog ofDemoRepository() {
    var table = RuntimeBinding.instantiateSoleImplementingClass(ISimpleGraphTableProvider.class).deriveDemoInstance();
    return new GraphTableDialog(table, /* repositorySnapshot */ null, /* windowTitle */ "Git Machete Help",
        /* windowWidth */800, /* windowHeight */ 250, /* okAction */ null, /* okButtonText */ "Close",
        /* cancelButtonVisible */ false);
  }

  private GraphTableDialog(JBTable table, @Nullable IGitMacheteRepositorySnapshot repositorySnapshot, String windowTitle,
      int windowWidth, int windowHeight, @Nullable Consumer<IGitMacheteRepositorySnapshot> okAction, String okButtonText,
      boolean cancelButtonVisible) {
    super(/* canBeParent */ false);

    this.table = table;
    this.repositorySnapshot = repositorySnapshot;
    this.windowWidth = windowWidth;
    this.windowHeight = windowHeight;
    this.okAction = okAction;
    this.cancelButtonVisible = cancelButtonVisible;

    // Note: since the class is final, `this` is already @Initialized at this point.
    init();
    setTitle(windowTitle);
    setOKButtonText(okButtonText);
  }

  @Override
  protected Action[] createActions() {
    return cancelButtonVisible
        ? new Action[]{getOKAction(), getCancelAction()}
        : new Action[]{getOKAction()};
  }

  @Override
  protected JComponent createCenterPanel() {
    var panel = JBUI.Panels.simplePanel(/* hgap */ 0, /* vgap */ 2);
    var scrollPane = ScrollPaneFactory.createScrollPane(table);
    panel.addToCenter(scrollPane);
    panel.setPreferredSize(new JBDimension(windowWidth, windowHeight));
    return panel;
  }

  @Override
  protected void doOKAction() {
    if (getOKAction().isEnabled()) {
      if (okAction != null && repositorySnapshot != null) {
        okAction.consume(repositorySnapshot);
      }
      close(OK_EXIT_CODE);
    }
  }
}
